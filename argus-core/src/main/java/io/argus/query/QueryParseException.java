package io.argus.query;

/** Thrown when a query string cannot be parsed. */
public class QueryParseException extends RuntimeException {

    public QueryParseException(String message) {
        super(message);
    }
}
