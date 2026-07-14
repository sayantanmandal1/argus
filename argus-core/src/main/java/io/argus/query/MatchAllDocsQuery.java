package io.argus.query;

import io.argus.index.IndexReader;
import io.argus.search.MatchAllScorer;
import io.argus.search.Query;
import io.argus.search.Scorer;
import io.argus.search.Similarity;

/** Matches every live document with a constant score. Useful as a base for pure filtering. */
public final class MatchAllDocsQuery extends Query {

    private final double boost;

    public MatchAllDocsQuery() {
        this(1.0);
    }

    public MatchAllDocsQuery(double boost) {
        this.boost = boost;
    }

    @Override
    public Scorer createScorer(IndexReader reader, Similarity similarity) {
        return new MatchAllScorer(reader, boost);
    }

    @Override
    public String toString() {
        return "*:*";
    }
}
