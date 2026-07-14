package io.argus.query;

/** How a {@link BooleanClause} participates in a boolean query. */
public enum Occur {
    /** The clause must match (AND). */
    MUST,
    /** The clause should match (OR); contributes to score and to minimum-should-match. */
    SHOULD,
    /** The clause must not match (NOT); vetoes any document it matches. */
    MUST_NOT
}
