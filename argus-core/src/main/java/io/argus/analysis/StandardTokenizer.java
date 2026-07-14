package io.argus.analysis;

/**
 * Splits text into maximal runs of letters and digits (Unicode-aware), discarding punctuation and
 * whitespace. Each emitted token records its character offsets in the source text.
 */
public final class StandardTokenizer extends Tokenizer {

    private final String text;
    private int pos;

    public StandardTokenizer(String text) {
        this.text = text == null ? "" : text;
    }

    @Override
    public boolean incrementToken() {
        int n = text.length();
        while (pos < n && !isTokenChar(text.charAt(pos))) {
            pos++;
        }
        if (pos >= n) {
            return false;
        }
        int start = pos;
        while (pos < n && isTokenChar(text.charAt(pos))) {
            pos++;
        }
        token.clear();
        token.setTerm(text.substring(start, pos));
        token.setOffsets(start, pos);
        token.setPositionIncrement(1);
        return true;
    }

    private static boolean isTokenChar(char c) {
        return Character.isLetterOrDigit(c);
    }

    @Override
    public void reset() {
        pos = 0;
    }
}
