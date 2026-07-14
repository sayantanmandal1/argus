package io.argus.index;

import java.util.Arrays;

/**
 * One entry in a {@link PostingList}: a document that contains a term, the term's frequency in that
 * document, and the positions at which it occurs (used for phrase queries).
 */
public final class Posting {

    private final int docId;
    private final int frequency;
    private final int[] positions;

    public Posting(int docId, int frequency, int[] positions) {
        if (docId < 0) {
            throw new IllegalArgumentException("docId must be >= 0: " + docId);
        }
        if (frequency <= 0) {
            throw new IllegalArgumentException("frequency must be > 0: " + frequency);
        }
        this.docId = docId;
        this.frequency = frequency;
        this.positions = positions == null ? new int[0] : positions;
    }

    public int docId() {
        return docId;
    }

    /** The number of times the term occurs in the document. */
    public int frequency() {
        return frequency;
    }

    /** The (ascending) token positions of the term within the document's field. */
    public int[] positions() {
        return positions;
    }

    @Override
    public String toString() {
        return "Posting{doc=" + docId + ", tf=" + frequency + ", pos=" + Arrays.toString(positions) + "}";
    }
}
