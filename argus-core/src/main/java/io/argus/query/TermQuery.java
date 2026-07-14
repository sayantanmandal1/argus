package io.argus.query;

import io.argus.index.IndexReader;
import io.argus.index.PostingList;
import io.argus.search.EmptyScorer;
import io.argus.search.Query;
import io.argus.search.Scorer;
import io.argus.search.Similarity;
import io.argus.search.TermScorer;
import java.util.Objects;

/**
 * Matches documents containing a single term in a field. The term is used verbatim — callers are
 * responsible for analyzing query text first (the {@link QueryParser} does this automatically), so
 * that a query term lines up with the analyzed terms stored in the index.
 */
public final class TermQuery extends Query {

    private final String field;
    private final String term;

    public TermQuery(String field, String term) {
        this.field = Objects.requireNonNull(field, "field");
        this.term = Objects.requireNonNull(term, "term");
    }

    public String field() {
        return field;
    }

    public String term() {
        return term;
    }

    @Override
    public Scorer createScorer(IndexReader reader, Similarity similarity) {
        PostingList postings = reader.postings(field, term);
        if (postings == null || postings.docFrequency() == 0) {
            return new EmptyScorer();
        }
        return new TermScorer(reader, field, postings, similarity);
    }

    @Override
    public String toString() {
        return field + ":" + term;
    }
}
