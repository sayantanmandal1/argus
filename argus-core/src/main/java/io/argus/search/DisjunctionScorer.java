package io.argus.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Union scorer: matches a document when at least {@code minimumShouldMatch} child scorers match it
 * (boolean SHOULD / OR). Child scorers are kept in a min-heap keyed by current document id, so the
 * next matching document is always found in O(log n). The score sums the matching children.
 */
public final class DisjunctionScorer extends Scorer {

    private final PriorityQueue<Scorer> heap;
    private final int minimumShouldMatch;
    private int docId = -1;

    public DisjunctionScorer(List<Scorer> optional, int minimumShouldMatch) {
        if (optional.isEmpty()) {
            throw new IllegalArgumentException("disjunction needs at least one scorer");
        }
        this.minimumShouldMatch = Math.max(1, minimumShouldMatch);
        this.heap = new PriorityQueue<>(optional.size(), Comparator.comparingInt(Scorer::docId));
        for (Scorer s : optional) {
            if (s.nextDoc() != NO_MORE_DOCS) {
                heap.add(s);
            }
        }
    }

    @Override
    public int docId() {
        return docId;
    }

    @Override
    public int nextDoc() {
        if (docId != -1) {
            while (!heap.isEmpty() && heap.peek().docId() == docId) {
                Scorer s = heap.poll();
                if (s.nextDoc() != NO_MORE_DOCS) {
                    heap.add(s);
                }
            }
        }
        return slideToNextMatch();
    }

    @Override
    public int advance(int target) {
        List<Scorer> buffer = new ArrayList<>();
        while (!heap.isEmpty() && heap.peek().docId() < target) {
            Scorer s = heap.poll();
            if (s.advance(target) != NO_MORE_DOCS) {
                buffer.add(s);
            }
        }
        heap.addAll(buffer);
        return slideToNextMatch();
    }

    private int slideToNextMatch() {
        while (!heap.isEmpty()) {
            int candidate = heap.peek().docId();
            if (countAt(candidate) >= minimumShouldMatch) {
                docId = candidate;
                return docId;
            }
            List<Scorer> buffer = new ArrayList<>();
            while (!heap.isEmpty() && heap.peek().docId() == candidate) {
                Scorer s = heap.poll();
                if (s.advance(candidate + 1) != NO_MORE_DOCS) {
                    buffer.add(s);
                }
            }
            heap.addAll(buffer);
        }
        docId = NO_MORE_DOCS;
        return NO_MORE_DOCS;
    }

    private int countAt(int candidate) {
        int count = 0;
        for (Scorer s : heap) {
            if (s.docId() == candidate) {
                count++;
            }
        }
        return count;
    }

    @Override
    public double score() {
        double sum = 0;
        for (Scorer s : heap) {
            if (s.docId() == docId) {
                sum += s.score();
            }
        }
        return sum;
    }

    @Override
    public long cost() {
        long c = 0;
        for (Scorer s : heap) {
            c += s.cost();
        }
        return c;
    }
}
