package io.argus.index;

import io.argus.analysis.Analyzer;
import io.argus.analysis.StandardAnalyzer;
import io.argus.document.Document;
import io.argus.search.IndexSearcher;
import io.argus.store.RAMDirectory;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A thread-safe indexing front-end with near-real-time (NRT) search.
 *
 * <p>Writes mutate a live index guarded by the underlying {@link IndexWriter}'s monitor. Searches run
 * against an <em>immutable snapshot</em> published by {@link #refresh()} into an
 * {@link AtomicReference}, so any number of reader threads can query concurrently and lock-free while
 * a writer thread keeps indexing — they simply won't see documents added since the last refresh.
 *
 * <p>{@link #bulkAdd} analyzes documents across a thread pool (analysis is CPU-heavy and stateless)
 * and appends them in order, so ingestion scales with cores while document ids stay deterministic.
 */
public final class ConcurrentIndexWriter implements Closeable {

    private final IndexWriter writer;
    private final ExecutorService pool;
    private final AtomicReference<InMemoryIndex> snapshot = new AtomicReference<>(new InMemoryIndex());

    public ConcurrentIndexWriter() {
        this(new StandardAnalyzer(), Runtime.getRuntime().availableProcessors());
    }

    public ConcurrentIndexWriter(Analyzer analyzer, int threads) {
        this.writer = new IndexWriter(analyzer);
        this.pool = Executors.newFixedThreadPool(Math.max(1, threads), r -> {
            Thread t = new Thread(r, "argus-indexer");
            t.setDaemon(true);
            return t;
        });
    }

    /** Analyzes and appends one document. Thread-safe. */
    public int addDocument(Document doc) {
        return writer.addDocument(doc);
    }

    /** Soft-deletes documents matching a term. Thread-safe. */
    public int deleteByTerm(String field, String term) {
        return writer.deleteByTerm(field, term);
    }

    /**
     * Analyzes {@code docs} in parallel and appends them in iteration order (so document ids remain
     * deterministic). Returns the assigned ids.
     */
    public List<Integer> bulkAdd(Collection<Document> docs) {
        List<Future<AnalyzedDocument>> analyzed = new ArrayList<>(docs.size());
        for (Document doc : docs) {
            analyzed.add(pool.submit(() -> writer.analyze(doc)));
        }
        List<Integer> ids = new ArrayList<>(docs.size());
        for (Future<AnalyzedDocument> f : analyzed) {
            ids.add(writer.append(get(f)));
        }
        return ids;
    }

    private static AnalyzedDocument get(Future<AnalyzedDocument> f) {
        try {
            return f.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("bulk indexing interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("document analysis failed", e.getCause());
        }
    }

    /** Publishes a fresh immutable snapshot reflecting every document indexed so far. */
    public void refresh() {
        synchronized (writer) {
            RAMDirectory tmp = new RAMDirectory();
            SegmentWriter.write(tmp, "snapshot", writer.index());
            snapshot.set(SegmentReader.load(tmp, "snapshot"));
        }
    }

    /** A searcher over the most recently refreshed snapshot (lock-free to acquire). */
    public IndexSearcher acquireSearcher() {
        return new IndexSearcher(snapshot.get());
    }

    /** The most recently refreshed reader. */
    public IndexReader currentReader() {
        return snapshot.get();
    }

    @Override
    public void close() {
        pool.shutdown();
    }
}
