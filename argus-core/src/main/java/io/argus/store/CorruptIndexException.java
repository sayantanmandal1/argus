package io.argus.store;

/** Thrown when stored data fails an integrity check (bad checksum, truncation, unknown format). */
public class CorruptIndexException extends RuntimeException {

    public CorruptIndexException(String message) {
        super(message);
    }
}
