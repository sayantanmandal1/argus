package io.argus.query;

import io.argus.index.IndexReader;
import io.argus.index.PostingList;
import io.argus.search.DisjunctionScorer;
import io.argus.search.EmptyScorer;
import io.argus.search.Query;
import io.argus.search.Scorer;
import io.argus.search.Similarity;
import io.argus.search.TermScorer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Matches every term in a field that begins with a given prefix (e.g. {@code dev*} matches
 * {@code develop}, {@code developer}, {@code development}). Expands to a union of the matching terms.
 */
public final class PrefixQuery extends Query {

    private final String field;
    private final String prefix;

    public PrefixQuery(String field, String prefix) {
        this.field = Objects.requireNonNull(field, "field");
        this.prefix = Objects.requireNonNull(prefix, "prefix");
    }

    public String field() {
        return field;
    }

    public String prefix() {
        return prefix;
    }

    @Override
    public Scorer createScorer(IndexReader reader, Similarity similarity) {
        List<Scorer> subs = new ArrayList<>();
        for (String term : reader.terms(field)) {
            if (term.startsWith(prefix)) {
                PostingList pl = reader.postings(field, term);
                if (pl != null && pl.docFrequency() > 0) {
                    subs.add(new TermScorer(reader, field, pl, similarity));
                }
            }
        }
        if (subs.isEmpty()) {
            return new EmptyScorer();
        }
        if (subs.size() == 1) {
            return subs.get(0);
        }
        return new DisjunctionScorer(subs, 1);
    }

    @Override
    public String toString() {
        return field + ":" + prefix + "*";
    }
}
