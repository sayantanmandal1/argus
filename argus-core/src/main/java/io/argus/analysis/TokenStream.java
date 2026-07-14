package io.argus.analysis;

/**
 * A pull-based stream of {@link Token}s. Callers repeatedly invoke {@link #incrementToken()} and,
 * when it returns {@code true}, read the current token from {@link #token()}. A single mutable token
 * instance is reused across calls to avoid per-token allocation.
 */
public abstract class TokenStream {

    /**
     * Advances to the next token.
     *
     * @return {@code true} if a token is available via {@link #token()}, {@code false} at end of stream
     */
    public abstract boolean incrementToken();

    /** The current token. Only valid after {@link #incrementToken()} returns {@code true}. */
    public abstract Token token();

    /** Resets the stream so it can be consumed again from the beginning. */
    public void reset() {
    }

    /** Releases any resources held by the stream. */
    public void close() {
    }
}
