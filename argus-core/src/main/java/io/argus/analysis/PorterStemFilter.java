package io.argus.analysis;

/** Applies the {@link PorterStemmer} to each token's term. */
public final class PorterStemFilter extends TokenFilter {

    private final PorterStemmer stemmer = new PorterStemmer();

    public PorterStemFilter(TokenStream input) {
        super(input);
    }

    @Override
    public boolean incrementToken() {
        if (!input.incrementToken()) {
            return false;
        }
        Token t = input.token();
        t.setTerm(stemmer.stem(t.term()));
        return true;
    }
}
