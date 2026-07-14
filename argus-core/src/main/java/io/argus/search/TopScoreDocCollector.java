package io.argus.search;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Keeps the top {@code k} hits by score using a bounded min-heap, so scoring a corpus of millions
 * of documents needs only O(k) memory and O(log k) work per hit. Ties break toward the lower
 * document id for stable, deterministic ordering.
 */
public final class TopScoreDocCollector {

    private final int k;
    private final PriorityQueue<ScoreDoc> heap;
    private long totalHits;

    public TopScoreDocCollector(int k) {
        if (k <= 0) {
            throw new IllegalArgumentException("k must be > 0: " + k);
        }
        this.k = k;
        // Min-heap: the weakest hit (lowest score, then highest docId) sits at the top for eviction.
        this.heap = new PriorityQueue<>(k, Comparator
                .comparingDouble((ScoreDoc s) -> s.score)
                .thenComparing(s -> s.docId, Comparator.reverseOrder()));
    }

    public void collect(int docId, double score) {
        totalHits++;
        if (heap.size() < k) {
            heap.add(new ScoreDoc(docId, score));
        } else {
            ScoreDoc weakest = heap.peek();
            if (score > weakest.score || (score == weakest.score && docId < weakest.docId)) {
                heap.poll();
                heap.add(new ScoreDoc(docId, score));
            }
        }
    }

    public long totalHits() {
        return totalHits;
    }

    /** Returns the collected hits sorted by descending score, ties broken by ascending doc id. */
    public TopDocs topDocs() {
        ScoreDoc[] docs = heap.toArray(new ScoreDoc[0]);
        Arrays.sort(docs, Comparator
                .comparingDouble((ScoreDoc s) -> s.score).reversed()
                .thenComparingInt(s -> s.docId));
        return new TopDocs(totalHits, docs);
    }
}
