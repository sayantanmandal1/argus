package io.argus.index;

import io.argus.analysis.Analyzer;
import io.argus.analysis.KeywordAnalyzer;
import io.argus.analysis.StandardAnalyzer;
import io.argus.analysis.Token;
import io.argus.document.Document;
import io.argus.document.Field;
import io.argus.document.FieldType;
import io.argus.util.IntArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builds an inverted index from {@link Document}s. For each indexed field it runs the appropriate
 * {@link Analyzer}, accumulates per-term positions, and appends postings to an {@link InMemoryIndex}.
 * Multi-valued fields are separated by a configurable position gap so phrases don't match across
 * values. Updates are modeled as delete-then-add via {@link #deleteByTerm}.
 */
public final class IndexWriter {

    /** Position gap inserted between separate values of a multi-valued field. */
    public static final int DEFAULT_POSITION_INCREMENT_GAP = 100;

    private final Analyzer textAnalyzer;
    private final Analyzer keywordAnalyzer = new KeywordAnalyzer(false);
    private final InMemoryIndex index;
    private final int positionIncrementGap;

    public IndexWriter() {
        this(new StandardAnalyzer(), DEFAULT_POSITION_INCREMENT_GAP);
    }

    public IndexWriter(Analyzer textAnalyzer) {
        this(textAnalyzer, DEFAULT_POSITION_INCREMENT_GAP);
    }

    public IndexWriter(Analyzer textAnalyzer, int positionIncrementGap) {
        this(textAnalyzer, positionIncrementGap, new InMemoryIndex());
    }

    /** Continues writing into an existing index (e.g. one loaded from disk by a segment reader). */
    IndexWriter(Analyzer textAnalyzer, int positionIncrementGap, InMemoryIndex index) {
        this.textAnalyzer = Objects.requireNonNull(textAnalyzer, "textAnalyzer");
        if (positionIncrementGap < 0) {
            throw new IllegalArgumentException("positionIncrementGap must be >= 0");
        }
        this.positionIncrementGap = positionIncrementGap;
        this.index = Objects.requireNonNull(index, "index");
    }

    /** Analyzes and indexes {@code doc}, returning its assigned document id. */
    public synchronized int addDocument(Document doc) {
        return append(analyze(doc));
    }

    /**
     * Analyzes a document into per-field, per-term positions without touching the index. It reads no
     * shared mutable state, so it is safe to run on many threads in parallel (see
     * {@link ConcurrentIndexWriter#bulkAdd}).
     */
    AnalyzedDocument analyze(Document doc) {
        Objects.requireNonNull(doc, "doc");
        Map<String, Map<String, IntArrayList>> fieldTermPositions = new LinkedHashMap<>();
        Map<String, Integer> nextPosition = new LinkedHashMap<>();
        Map<String, Integer> fieldTokenCount = new LinkedHashMap<>();

        for (Field f : doc.fields()) {
            if (!f.type().isIndexed()) {
                continue;
            }
            Analyzer analyzer = (f.type() == FieldType.KEYWORD) ? keywordAnalyzer : textAnalyzer;
            List<Token> tokens = analyzer.analyze(f.name(), f.value());
            Map<String, IntArrayList> termPositions =
                    fieldTermPositions.computeIfAbsent(f.name(), k -> new LinkedHashMap<>());
            int pos = nextPosition.getOrDefault(f.name(), -1);
            int count = 0;
            for (Token t : tokens) {
                pos += t.positionIncrement();
                termPositions.computeIfAbsent(t.term(), k -> new IntArrayList()).add(pos);
                count++;
            }
            nextPosition.put(f.name(), pos + positionIncrementGap);
            fieldTokenCount.merge(f.name(), count, Integer::sum);
        }
        return new AnalyzedDocument(doc, fieldTermPositions, fieldTokenCount);
    }

    /** Appends a pre-analyzed document to the index. Serialized to keep the index consistent. */
    synchronized int append(AnalyzedDocument analyzed) {
        int docId = index.newDocId(analyzed.document());
        for (Map.Entry<String, Map<String, IntArrayList>> fe : analyzed.fieldTermPositions().entrySet()) {
            String field = fe.getKey();
            for (Map.Entry<String, IntArrayList> te : fe.getValue().entrySet()) {
                IntArrayList positions = te.getValue();
                index.addPosting(field, te.getKey(),
                        new Posting(docId, positions.size(), positions.toArray()));
            }
            index.recordFieldLength(docId, field, analyzed.fieldTokenCount().getOrDefault(field, 0));
        }
        return docId;
    }

    /** Package-private access to the live index for snapshotting by the concurrent writer. */
    InMemoryIndex index() {
        return index;
    }

    /** Soft-deletes every document containing {@code term} in {@code field}; returns the count. */
    public synchronized int deleteByTerm(String field, String term) {
        PostingList pl = index.postings(field, term);
        if (pl == null) {
            return 0;
        }
        int deleted = 0;
        for (Posting p : pl.postings()) {
            if (!index.isDeleted(p.docId())) {
                index.markDeleted(p.docId());
                deleted++;
            }
        }
        return deleted;
    }

    public synchronized void deleteDocument(int docId) {
        index.markDeleted(docId);
    }

    /** Returns a reader over the current index state. */
    public IndexReader getReader() {
        return index;
    }

    public int numDocs() {
        return index.numDocs();
    }

    public int maxDoc() {
        return index.maxDoc();
    }
}
