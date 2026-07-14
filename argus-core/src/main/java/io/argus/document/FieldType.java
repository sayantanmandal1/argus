package io.argus.document;

/**
 * Describes how a {@link Field}'s value is handled by the engine.
 *
 * <ul>
 *   <li>{@link #TEXT} — analyzed into terms, indexed with positions, and stored. Free text.</li>
 *   <li>{@link #KEYWORD} — indexed verbatim as a single term (no analysis) and stored. Ids/tags.</li>
 *   <li>{@link #STORED} — stored and returned with hits, but never indexed or searchable.</li>
 * </ul>
 */
public enum FieldType {
    TEXT(true, true, true),
    KEYWORD(true, false, true),
    STORED(false, false, true);

    private final boolean indexed;
    private final boolean analyzed;
    private final boolean stored;

    FieldType(boolean indexed, boolean analyzed, boolean stored) {
        this.indexed = indexed;
        this.analyzed = analyzed;
        this.stored = stored;
    }

    /** Whether the field's terms are added to the inverted index and are searchable. */
    public boolean isIndexed() {
        return indexed;
    }

    /** Whether the field's value is run through an {@code Analyzer} before indexing. */
    public boolean isAnalyzed() {
        return analyzed;
    }

    /** Whether the raw value is retained and returned with search hits. */
    public boolean isStored() {
        return stored;
    }
}
