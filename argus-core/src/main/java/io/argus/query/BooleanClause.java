package io.argus.query;

import io.argus.search.Query;
import java.util.Objects;

/** A sub-query of a {@link BooleanQuery} together with its {@link Occur} role. */
public final class BooleanClause {

    private final Query query;
    private final Occur occur;

    public BooleanClause(Query query, Occur occur) {
        this.query = Objects.requireNonNull(query, "query");
        this.occur = Objects.requireNonNull(occur, "occur");
    }

    public Query query() {
        return query;
    }

    public Occur occur() {
        return occur;
    }

    @Override
    public String toString() {
        String prefix = switch (occur) {
            case MUST -> "+";
            case MUST_NOT -> "-";
            case SHOULD -> "";
        };
        return prefix + "(" + query + ")";
    }
}
