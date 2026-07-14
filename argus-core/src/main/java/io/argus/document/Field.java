package io.argus.document;

import java.util.Objects;

/**
 * A single named value within a {@link Document}, tagged with a {@link FieldType} that decides
 * whether it is analyzed, indexed, and/or stored.
 */
public final class Field {

    private final String name;
    private final String value;
    private final FieldType type;
    private final float boost;

    public Field(String name, String value, FieldType type) {
        this(name, value, type, 1.0f);
    }

    public Field(String name, String value, FieldType type, float boost) {
        this.name = Objects.requireNonNull(name, "name");
        this.value = Objects.requireNonNull(value, "value");
        this.type = Objects.requireNonNull(type, "type");
        if (boost <= 0f || Float.isNaN(boost) || Float.isInfinite(boost)) {
            throw new IllegalArgumentException("boost must be a finite positive number: " + boost);
        }
        this.boost = boost;
    }

    /** A free-text, analyzed field. */
    public static Field text(String name, String value) {
        return new Field(name, value, FieldType.TEXT);
    }

    /** An un-analyzed keyword field (single indexed term). */
    public static Field keyword(String name, String value) {
        return new Field(name, value, FieldType.KEYWORD);
    }

    /** A stored-only field (returned with hits, not searchable). */
    public static Field stored(String name, String value) {
        return new Field(name, value, FieldType.STORED);
    }

    public String name() {
        return name;
    }

    public String value() {
        return value;
    }

    public FieldType type() {
        return type;
    }

    /** Per-field score multiplier applied at index time; defaults to 1.0. */
    public float boost() {
        return boost;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Field other)) {
            return false;
        }
        return Float.compare(boost, other.boost) == 0
                && name.equals(other.name)
                && value.equals(other.value)
                && type == other.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value, type, boost);
    }

    @Override
    public String toString() {
        return name + "=\"" + value + "\" (" + type + (boost == 1.0f ? "" : ", boost=" + boost) + ")";
    }
}
