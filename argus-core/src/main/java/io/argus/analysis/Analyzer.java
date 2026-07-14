package io.argus.analysis;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns raw field text into a sequence of index terms. Concrete analyzers assemble a
 * {@link Tokenizer} and a chain of {@link TokenFilter}s; the same analyzer must be used at index
 * time and query time so that terms line up.
 */
public abstract class Analyzer {

    /** Builds the token stream for a field's text. */
    protected abstract TokenStream createTokenStream(String field, String text);

    /** Fully analyzes {@code text} into an ordered list of independent tokens. */
    public final List<Token> analyze(String field, String text) {
        List<Token> out = new ArrayList<>();
        TokenStream ts = createTokenStream(field, text == null ? "" : text);
        ts.reset();
        try {
            while (ts.incrementToken()) {
                out.add(ts.token().copy());
            }
        } finally {
            ts.close();
        }
        return out;
    }

    /** Convenience: analyze and return just the term strings. */
    public final List<String> terms(String field, String text) {
        List<Token> tokens = analyze(field, text);
        List<String> terms = new ArrayList<>(tokens.size());
        for (Token t : tokens) {
            terms.add(t.term());
        }
        return terms;
    }
}
