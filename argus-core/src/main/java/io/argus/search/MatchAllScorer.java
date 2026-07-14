package io.argus.search;

import io.argus.index.IndexReader;

/** Matches every live document with a constant score; the execution side of a match-all query. */
public final class MatchAllScorer extends Scorer {

    private final IndexReader reader;
    private final int maxDoc;
    private final double score;
    private int docId = -1;

    public MatchAllScorer(IndexReader reader, double score) {
        this.reader = reader;
        this.maxDoc = reader.maxDoc();
        this.score = score;
    }

    @Override
    public int docId() {
        return docId;
    }

    @Override
    public int nextDoc() {
        if (docId == NO_MORE_DOCS) {
            return NO_MORE_DOCS;
        }
        return advance(docId + 1);
    }

    @Override
    public int advance(int target) {
        int d = Math.max(target, 0);
        while (d < maxDoc) {
            if (!reader.isDeleted(d)) {
                docId = d;
                return d;
            }
            d++;
        }
        docId = NO_MORE_DOCS;
        return NO_MORE_DOCS;
    }

    @Override
    public double score() {
        return score;
    }

    @Override
    public long cost() {
        return maxDoc;
    }
}
