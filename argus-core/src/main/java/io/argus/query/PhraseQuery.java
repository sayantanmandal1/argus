package io.argus.query;

import io.argus.index.IndexReader;
import io.argus.index.PostingList;
import io.argus.search.EmptyScorer;
import io.argus.search.PhraseScorer;
import io.argus.search.Query;
import io.argus.search.Scorer;
import io.argus.search.Similarity;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Matches an exact, consecutive sequence of terms in a field (e.g. {@code "distributed systems"}).
 * Terms must already be analyzed. A single-term phrase degrades to a plain term query.
 */
public final class PhraseQuery extends Query {

    private final String field;
    private final List<String> terms;

    public PhraseQuery(String field, List<String> terms) {
        this.field = Objects.requireNonNull(field, "field");
        this.terms = List.copyOf(terms);
    }

    public String field() {
        return field;
    }

    public List<String> terms() {
        return terms;
    }

    @Override
    public Scorer createScorer(IndexReader reader, Similarity similarity) {
        if (terms.isEmpty()) {
            return new EmptyScorer();
        }
        if (terms.size() == 1) {
            return new TermQuery(field, terms.get(0)).createScorer(reader, similarity);
        }
        List<PostingList> lists = new ArrayList<>(terms.size());
        for (String term : terms) {
            PostingList pl = reader.postings(field, term);
            if (pl == null || pl.docFrequency() == 0) {
                return new EmptyScorer();
            }
            lists.add(pl);
        }
        return new PhraseScorer(reader, field, lists, similarity);
    }

    @Override
    public String toString() {
        return field + ":\"" + String.join(" ", terms) + "\"";
    }
}
