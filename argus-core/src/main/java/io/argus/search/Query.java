package io.argus.search;

import io.argus.index.IndexReader;

/**
 * The base type for all queries. A query knows how to build a {@link Scorer} over a given
 * {@link IndexReader} and {@link Similarity}; the {@link IndexSearcher} drives that scorer to
 * collect hits. Concrete query types (term, boolean, phrase, prefix, match-all) live in the
 * {@code io.argus.query} package.
 */
public abstract class Query {

    /** Builds a scorer that enumerates and scores the documents this query matches. */
    public abstract Scorer createScorer(IndexReader reader, Similarity similarity);

    @Override
    public abstract String toString();
}
