package io.argus.document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * An ordered collection of {@link Field}s — the unit of indexing and retrieval. A document may hold
 * several fields with the same name (e.g. multiple {@code tag} values).
 */
public final class Document {

    private final List<Field> fields = new ArrayList<>();

    public Document add(Field field) {
        fields.add(Objects.requireNonNull(field, "field"));
        return this;
    }

    public Document addText(String name, String value) {
        return add(Field.text(name, value));
    }

    public Document addKeyword(String name, String value) {
        return add(Field.keyword(name, value));
    }

    public Document addStored(String name, String value) {
        return add(Field.stored(name, value));
    }

    /** All fields, in insertion order. */
    public List<Field> fields() {
        return Collections.unmodifiableList(fields);
    }

    /** All fields with the given name, in insertion order. */
    public List<Field> fields(String name) {
        List<Field> out = new ArrayList<>();
        for (Field f : fields) {
            if (f.name().equals(name)) {
                out.add(f);
            }
        }
        return out;
    }

    /** The value of the first field with the given name, or {@code null} if absent. */
    public String get(String name) {
        for (Field f : fields) {
            if (f.name().equals(name)) {
                return f.value();
            }
        }
        return null;
    }

    public boolean isEmpty() {
        return fields.isEmpty();
    }

    public int size() {
        return fields.size();
    }

    @Override
    public String toString() {
        return "Document" + fields;
    }
}
