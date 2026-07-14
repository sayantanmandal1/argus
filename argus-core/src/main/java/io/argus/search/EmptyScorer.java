package io.argus.search;

/** A scorer that matches nothing; used for terms that are absent from the index. */
public final class EmptyScorer extends Scorer {

    @Override
    public int docId() {
        return NO_MORE_DOCS;
    }

    @Override
    public int nextDoc() {
        return NO_MORE_DOCS;
    }

    @Override
    public int advance(int target) {
        return NO_MORE_DOCS;
    }

    @Override
    public double score() {
        return 0.0;
    }

    @Override
    public long cost() {
        return 0;
    }
}
