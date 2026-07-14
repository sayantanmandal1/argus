package io.argus.search;

import java.util.List;

/**
 * Composes a boolean query's clauses: a required {@code base} (a conjunction of MUST clauses or a
 * disjunction of SHOULD clauses), optional SHOULD scorers that only add to the score, and prohibited
 * MUST_NOT scorers that veto a document. Prohibited and optional scorers are advanced lazily to the
 * current document, so they cost nothing on documents the base never proposes.
 */
public final class BooleanScorer extends Scorer {

    private final Scorer base;
    private final List<Scorer> optionalForScoring;
    private final List<Scorer> prohibited;
    private int docId = -1;

    public BooleanScorer(Scorer base, List<Scorer> optionalForScoring, List<Scorer> prohibited) {
        this.base = base;
        this.optionalForScoring = optionalForScoring;
        this.prohibited = prohibited;
    }

    @Override
    public int docId() {
        return docId;
    }

    @Override
    public int nextDoc() {
        return toAllowed(base.nextDoc());
    }

    @Override
    public int advance(int target) {
        return toAllowed(base.advance(target));
    }

    private int toAllowed(int candidate) {
        while (candidate != NO_MORE_DOCS) {
            if (!excluded(candidate)) {
                docId = candidate;
                return docId;
            }
            candidate = base.nextDoc();
        }
        docId = NO_MORE_DOCS;
        return NO_MORE_DOCS;
    }

    private boolean excluded(int doc) {
        for (Scorer p : prohibited) {
            int d = p.docId();
            if (d < doc) {
                d = p.advance(doc);
            }
            if (d == doc) {
                return true;
            }
        }
        return false;
    }

    @Override
    public double score() {
        double sum = base.score();
        for (Scorer o : optionalForScoring) {
            int d = o.docId();
            if (d < docId) {
                d = o.advance(docId);
            }
            if (d == docId) {
                sum += o.score();
            }
        }
        return sum;
    }

    @Override
    public long cost() {
        return base.cost();
    }
}
