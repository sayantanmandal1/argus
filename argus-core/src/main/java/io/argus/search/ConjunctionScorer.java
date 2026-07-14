package io.argus.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Intersection scorer: matches only documents that every child scorer matches (boolean MUST / AND).
 * Advances the lowest-cost child first and leap-frogs the others to its document, which keeps the
 * join close to the cost of the rarest term. The score is the sum of the child scores.
 */
public final class ConjunctionScorer extends Scorer {

    private final Scorer[] scorers;
    private int docId = -1;

    public ConjunctionScorer(List<Scorer> required) {
        if (required.isEmpty()) {
            throw new IllegalArgumentException("conjunction needs at least one scorer");
        }
        this.scorers = required.toArray(new Scorer[0]);
        // Lead with the rarest (lowest-cost) scorer to minimize advance() calls.
        java.util.Arrays.sort(scorers, Comparator.comparingLong(Scorer::cost));
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
        return align(scorers[0].nextDoc());
    }

    @Override
    public int advance(int target) {
        if (docId == NO_MORE_DOCS) {
            return NO_MORE_DOCS;
        }
        return align(scorers[0].advance(target));
    }

    private int align(int candidate) {
        while (candidate != NO_MORE_DOCS) {
            boolean matched = true;
            for (int i = 1; i < scorers.length; i++) {
                int d = scorers[i].docId();
                if (d < candidate) {
                    d = scorers[i].advance(candidate);
                }
                if (d != candidate) {
                    candidate = scorers[0].advance(d);
                    matched = false;
                    break;
                }
            }
            if (matched) {
                docId = candidate;
                return docId;
            }
        }
        docId = NO_MORE_DOCS;
        return NO_MORE_DOCS;
    }

    @Override
    public double score() {
        double sum = 0;
        for (Scorer s : scorers) {
            sum += s.score();
        }
        return sum;
    }

    @Override
    public long cost() {
        return scorers[0].cost();
    }

    static List<Scorer> asList(Scorer[] arr) {
        return new ArrayList<>(java.util.Arrays.asList(arr));
    }
}
