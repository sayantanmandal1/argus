package io.argus.query;

import io.argus.index.IndexReader;
import io.argus.search.BooleanScorer;
import io.argus.search.ConjunctionScorer;
import io.argus.search.DisjunctionScorer;
import io.argus.search.EmptyScorer;
import io.argus.search.Query;
import io.argus.search.Scorer;
import io.argus.search.Similarity;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * A boolean combination of sub-queries. MUST clauses intersect, SHOULD clauses union (subject to
 * {@code minimumShouldMatch}), and MUST_NOT clauses exclude. When MUST clauses are present, SHOULD
 * clauses contribute only to the score. Built via the fluent {@link Builder}.
 */
public final class BooleanQuery extends Query {

    private final List<BooleanClause> clauses;
    private final int minimumShouldMatch;

    private BooleanQuery(List<BooleanClause> clauses, int minimumShouldMatch) {
        this.clauses = List.copyOf(clauses);
        this.minimumShouldMatch = minimumShouldMatch;
    }

    public List<BooleanClause> clauses() {
        return clauses;
    }

    public int minimumShouldMatch() {
        return minimumShouldMatch;
    }

    @Override
    public Scorer createScorer(IndexReader reader, Similarity similarity) {
        List<Scorer> required = new ArrayList<>();
        List<Scorer> optional = new ArrayList<>();
        List<Scorer> prohibited = new ArrayList<>();
        for (BooleanClause c : clauses) {
            Scorer sc = c.query().createScorer(reader, similarity);
            switch (c.occur()) {
                case MUST -> required.add(sc);
                case SHOULD -> optional.add(sc);
                case MUST_NOT -> prohibited.add(sc);
            }
        }

        Scorer base;
        List<Scorer> optionalForScoring;
        if (!required.isEmpty()) {
            base = required.size() == 1 ? required.get(0) : new ConjunctionScorer(required);
            optionalForScoring = optional;
        } else if (!optional.isEmpty()) {
            base = new DisjunctionScorer(optional, Math.max(minimumShouldMatch, 1));
            optionalForScoring = List.of();
        } else {
            return new EmptyScorer();
        }

        if (prohibited.isEmpty() && optionalForScoring.isEmpty()) {
            return base;
        }
        return new BooleanScorer(base, optionalForScoring, prohibited);
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(" ", "(", ")");
        for (BooleanClause c : clauses) {
            sj.add(c.toString());
        }
        return sj.toString();
    }

    /** Fluent builder for {@link BooleanQuery}. */
    public static final class Builder {
        private final List<BooleanClause> clauses = new ArrayList<>();
        private int minimumShouldMatch;

        public Builder add(Query query, Occur occur) {
            clauses.add(new BooleanClause(query, occur));
            return this;
        }

        public Builder must(Query query) {
            return add(query, Occur.MUST);
        }

        public Builder should(Query query) {
            return add(query, Occur.SHOULD);
        }

        public Builder mustNot(Query query) {
            return add(query, Occur.MUST_NOT);
        }

        public Builder minimumShouldMatch(int minimumShouldMatch) {
            this.minimumShouldMatch = minimumShouldMatch;
            return this;
        }

        public BooleanQuery build() {
            return new BooleanQuery(clauses, minimumShouldMatch);
        }
    }
}
