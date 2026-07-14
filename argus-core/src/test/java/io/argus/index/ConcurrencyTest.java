package io.argus.index;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.argus.analysis.StandardAnalyzer;
import io.argus.document.Document;
import io.argus.query.TermQuery;
import io.argus.search.IndexSearcher;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ConcurrencyTest {

    @Test
    void concurrentAddsAreThreadSafe() throws Exception {
        ConcurrentIndexWriter w = new ConcurrentIndexWriter(new StandardAnalyzer(), 8);
        int threads = 8;
        int perThread = 100;
        ExecutorService ex = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            final int tid = t;
            futures.add(ex.submit(() -> {
                start.await();
                for (int i = 0; i < perThread; i++) {
                    w.addDocument(new Document()
                            .addKeyword("id", tid + "-" + i)
                            .addText("body", "concurrent indexing test document"));
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> f : futures) {
            f.get();
        }
        ex.shutdown();

        w.refresh();
        assertEquals(threads * perThread, w.currentReader().numDocs());
        IndexSearcher s = w.acquireSearcher();
        assertEquals(threads * perThread, s.search(new TermQuery("body", "concurr"), 10).totalHits);
        w.close();
    }

    @Test
    void searchSeesDocumentsOnlyAfterRefresh() {
        ConcurrentIndexWriter w = new ConcurrentIndexWriter(new StandardAnalyzer(), 2);
        w.addDocument(new Document().addText("body", "hello world"));
        assertEquals(0, w.acquireSearcher().search(new TermQuery("body", "hello"), 10).totalHits);
        w.refresh();
        assertEquals(1, w.acquireSearcher().search(new TermQuery("body", "hello"), 10).totalHits);
        w.close();
    }

    @Test
    void bulkAddIndexesAllDocumentsInOrder() {
        ConcurrentIndexWriter w = new ConcurrentIndexWriter(new StandardAnalyzer(), 4);
        List<Document> docs = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            docs.add(new Document().addKeyword("id", "" + i)
                    .addText("body", "bulk parallel analysis document number " + i));
        }
        List<Integer> ids = w.bulkAdd(docs);
        assertEquals(200, ids.size());
        assertEquals(0, ids.get(0));
        assertEquals(199, ids.get(199));

        w.refresh();
        assertEquals(200, w.currentReader().numDocs());
        assertEquals(200, w.acquireSearcher().search(new TermQuery("body", "bulk"), 10).totalHits);
        w.close();
    }

    @Test
    void concurrentSearchesWhileIndexingNeverCorrupt() throws Exception {
        ConcurrentIndexWriter w = new ConcurrentIndexWriter(new StandardAnalyzer(), 4);
        AtomicBoolean stop = new AtomicBoolean(false);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Thread writer = new Thread(() -> {
            for (int i = 0; i < 500; i++) {
                w.addDocument(new Document().addText("body", "streaming document " + i));
                if (i % 50 == 0) {
                    w.refresh();
                }
            }
            w.refresh();
        });

        List<Thread> readers = new ArrayList<>();
        for (int r = 0; r < 4; r++) {
            Thread reader = new Thread(() -> {
                try {
                    while (!stop.get()) {
                        w.acquireSearcher().search(new TermQuery("body", "stream"), 5);
                    }
                } catch (Throwable t) {
                    error.set(t);
                }
            });
            readers.add(reader);
        }

        writer.start();
        readers.forEach(Thread::start);
        writer.join();
        stop.set(true);
        for (Thread reader : readers) {
            reader.join();
        }

        assertNull(error.get(), "readers must never observe a corrupt snapshot");
        w.refresh();
        assertEquals(500, w.currentReader().numDocs());
        w.close();
    }
}
