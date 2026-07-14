package io.argus.analysis;

/**
 * Base class for stream sources that produce tokens directly from input text. A tokenizer owns the
 * mutable {@link Token} that flows through any downstream {@link TokenFilter}s.
 */
public abstract class Tokenizer extends TokenStream {

    protected final Token token = new Token();

    @Override
    public Token token() {
        return token;
    }
}
