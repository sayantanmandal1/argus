package io.argus.index;

import io.argus.document.Document;
import io.argus.util.IntArrayList;
import java.util.Map;

/**
 * The output of analyzing one {@link Document}: the stored document plus its per-field, per-term
 * positions and token counts. It carries no index state, so it can be produced on a worker thread
 * and later appended on the writer thread.
 */
final class AnalyzedDocument {

    private final Document document;
    private final Map<String, Map<String, IntArrayList>> fieldTermPositions;
    private final Map<String, Integer> fieldTokenCount;

    AnalyzedDocument(Document document,
                     Map<String, Map<String, IntArrayList>> fieldTermPositions,
                     Map<String, Integer> fieldTokenCount) {
        this.document = document;
        this.fieldTermPositions = fieldTermPositions;
        this.fieldTokenCount = fieldTokenCount;
    }

    Document document() {
        return document;
    }

    Map<String, Map<String, IntArrayList>> fieldTermPositions() {
        return fieldTermPositions;
    }

    Map<String, Integer> fieldTokenCount() {
        return fieldTokenCount;
    }
}
