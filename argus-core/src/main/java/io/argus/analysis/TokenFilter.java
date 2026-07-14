package io.argus.analysis;

import java.util.Objects;

/**
 * Base class for stream stages that consume tokens from an upstream {@link TokenStream} and
 * transform them. Filters share the upstream's token instance so mutations flow through the chain
 * without copying.
 */
public abstract class TokenFilter extends TokenStream {

    protected final TokenStream input;

    protected TokenFilter(TokenStream input) {
        this.input = Objects.requireNonNull(input, "input");
    }

    @Override
    public Token token() {
        return input.token();
    }

    @Override
    public void reset() {
        input.reset();
    }

    @Override
    public void close() {
        input.close();
    }
}
