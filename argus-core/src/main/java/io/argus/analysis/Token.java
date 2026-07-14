package io.argus.analysis;

/**
 * A single token produced by the analysis pipeline: the term text plus its character offsets in the
 * original input and its position increment relative to the previous token.
 *
 * <p>The position increment is normally 1. A {@link StopFilter} that drops a token folds that
 * token's increment into the next surviving token, which keeps phrase-query positions accurate.
 */
public final class Token {

    private String term = "";
    private int startOffset;
    private int endOffset;
    private int positionIncrement = 1;

    public void clear() {
        term = "";
        startOffset = 0;
        endOffset = 0;
        positionIncrement = 1;
    }

    public String term() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term == null ? "" : term;
    }

    public int startOffset() {
        return startOffset;
    }

    public int endOffset() {
        return endOffset;
    }

    public void setOffsets(int startOffset, int endOffset) {
        if (startOffset < 0 || endOffset < startOffset) {
            throw new IllegalArgumentException("invalid offsets: " + startOffset + ".." + endOffset);
        }
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }

    public int positionIncrement() {
        return positionIncrement;
    }

    public void setPositionIncrement(int positionIncrement) {
        if (positionIncrement < 0) {
            throw new IllegalArgumentException("positionIncrement must be >= 0: " + positionIncrement);
        }
        this.positionIncrement = positionIncrement;
    }

    /** Returns an independent copy; used when materializing a stream into a list. */
    public Token copy() {
        Token t = new Token();
        t.term = term;
        t.startOffset = startOffset;
        t.endOffset = endOffset;
        t.positionIncrement = positionIncrement;
        return t;
    }

    @Override
    public String toString() {
        return "Token{'" + term + "'@" + startOffset + "-" + endOffset + ",posInc=" + positionIncrement + "}";
    }
}
