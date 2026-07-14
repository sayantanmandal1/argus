package io.argus.server;

import io.argus.analysis.StandardAnalyzer;
import io.argus.document.Document;
import io.argus.document.Field;
import io.argus.index.IndexReader;
import io.argus.index.InMemoryIndex;
import io.argus.index.PersistentIndex;
import io.argus.index.SegmentReader;
import io.argus.index.SegmentWriter;
import io.argus.query.QueryParser;
import io.argus.search.IndexSearcher;
import io.argus.search.Query;
import io.argus.search.ScoreDoc;
import io.argus.search.TopDocs;
import io.argus.store.RAMDirectory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The application service behind the HTTP API. Wraps a durable {@link PersistentIndex} and serves
 * searches from an immutable near-real-time snapshot, so concurrent HTTP request threads can query
 * safely while documents are being indexed. The snapshot is rebuilt lazily whenever the index has
 * changed since the last search.
 */
public final class SearchService {

    private final PersistentIndex index;
    private final StandardAnalyzer analyzer = new StandardAnalyzer();
    private final String defaultField;
    private final AtomicReference<IndexReader> snapshot = new AtomicReference<>(new InMemoryIndex());
    private volatile boolean dirty = true;

    public SearchService(PersistentIndex index, String defaultField) {
        this.index = index;
        this.defaultField = defaultField;
    }

    public synchronized int index(Map<String, Object> json) {
        int id = index.addDocument(toDocument(json));
        dirty = true;
        return id;
    }

    public synchronized int deleteByTerm(String field, String term) {
        int n = index.deleteByTerm(field, term);
        dirty = true;
        return n;
    }

    public synchronized void commit() {
        index.commit();
        dirty = true;
    }

    public Map<String, Object> search(String queryString, String field, int k) {
        String f = (field == null || field.isBlank()) ? defaultField : field;
        Query query = (queryString == null || queryString.isBlank())
                ? new io.argus.query.MatchAllDocsQuery()
                : new QueryParser(f, analyzer).parse(queryString);
        IndexReader reader = currentSnapshot();
        TopDocs top = new IndexSearcher(reader).search(query, Math.max(1, k));

        List<Object> hits = new ArrayList<>();
        for (ScoreDoc sd : top.scoreDocs) {
            Map<String, Object> hit = new LinkedHashMap<>();
            hit.put("docId", sd.docId);
            hit.put("score", sd.score);
            hit.put("fields", docToMap(reader.document(sd.docId)));
            hits.add(hit);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("query", query.toString());
        result.put("total", top.totalHits);
        result.put("returned", hits.size());
        result.put("hits", hits);
        return result;
    }

    public Map<String, Object> stats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("numDocs", index.numDocs());
        m.put("maxDoc", index.maxDoc());
        m.put("generation", index.generation());
        return m;
    }

    private IndexReader currentSnapshot() {
        if (dirty) {
            synchronized (this) {
                if (dirty) {
                    RAMDirectory tmp = new RAMDirectory();
                    SegmentWriter.write(tmp, "snapshot", index.getReader());
                    snapshot.set(SegmentReader.load(tmp, "snapshot"));
                    dirty = false;
                }
            }
        }
        return snapshot.get();
    }

    private Document toDocument(Map<String, Object> json) {
        Document doc = new Document();
        for (Map.Entry<String, Object> e : json.entrySet()) {
            if (e.getValue() == null) {
                continue;
            }
            String value = String.valueOf(e.getValue());
            if (e.getKey().equals("id") || e.getKey().equals("_id")) {
                doc.addKeyword("id", value);
            } else {
                doc.addText(e.getKey(), value);
            }
        }
        return doc;
    }

    private Map<String, Object> docToMap(Document doc) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (doc != null) {
            for (Field f : doc.fields()) {
                m.putIfAbsent(f.name(), f.value());
            }
        }
        return m;
    }
}
