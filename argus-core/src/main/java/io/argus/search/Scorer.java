package io.argus.search;

/**
 * Document-at-a-time iterator over the documents a query matches, exposing the current document's
 * score. Composite scorers (conjunction, disjunction, phrase) are built by combining child scorers,
 * which is how boolean and phrase queries execute efficiently over sorted postings.
 */
public abstract class Scorer {

    /** Sentinel returned once the scorer is exhausted. */
    public static final int NO_MORE_DOCS = Integer.MAX_VALUE;

    /** The current document id; {@code -1} before the first {@link #nextDoc()}. */
    public abstract int docId();

    /** Advances to the next matching document, or {@link #NO_MORE_DOCS} if none remain. */
    public abstract int nextDoc();

    /** Advances to the first matching document with id {@code >= target}. */
    public abstract int advance(int target);

    /** The score of the current document. Valid only after a successful advance. */
    public abstract double score();

    /** An estimate of the number of documents this scorer will match (used to order joins). */
    public long cost() {
        return Long.MAX_VALUE;
    }
}
