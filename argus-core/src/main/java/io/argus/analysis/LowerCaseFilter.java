package io.argus.analysis;

import java.util.Locale;

/** Lowercases each token's term using the root locale for stable, locale-independent behavior. */
public final class LowerCaseFilter extends TokenFilter {

    public LowerCaseFilter(TokenStream input) {
        super(input);
    }

    @Override
    public boolean incrementToken() {
        if (!input.incrementToken()) {
            return false;
        }
        Token t = input.token();
        t.setTerm(t.term().toLowerCase(Locale.ROOT));
        return true;
    }
}
