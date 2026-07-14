package io.argus.search;

/**
 * Okapi BM25 — the modern default ranking function. It rewards term frequency with diminishing
 * returns (controlled by {@code k1}) and normalizes for document length (controlled by {@code b}),
 * so a short document matching a rare term outranks a long one that merely repeats a common word.
 *
 * <pre>
 *   idf(t)   = ln(1 + (N - df + 0.5) / (df + 0.5))
 *   score    = idf * (tf * (k1 + 1)) / (tf + k1 * (1 - b + b * |d| / avgdl))
 * </pre>
 */
public final class BM25Similarity extends Similarity {

    private final double k1;
    private final double b;

    public BM25Similarity() {
        this(1.2, 0.75);
    }

    public BM25Similarity(double k1, double b) {
        if (k1 < 0 || Double.isNaN(k1)) {
            throw new IllegalArgumentException("k1 must be >= 0: " + k1);
        }
        if (b < 0 || b > 1 || Double.isNaN(b)) {
            throw new IllegalArgumentException("b must be in [0, 1]: " + b);
        }
        this.k1 = k1;
        this.b = b;
    }

    @Override
    public double idf(long docFreq, long docCount) {
        return Math.log(1.0 + (docCount - docFreq + 0.5) / (docFreq + 0.5));
    }

    @Override
    public double score(int termFreq, double idf, int fieldLength, double avgFieldLength) {
        double norm;
        if (avgFieldLength <= 0.0) {
            norm = 1.0;
        } else {
            norm = 1.0 - b + b * (fieldLength / avgFieldLength);
        }
        return idf * (termFreq * (k1 + 1.0)) / (termFreq + k1 * norm);
    }

    public double k1() {
        return k1;
    }

    public double b() {
        return b;
    }

    @Override
    public String toString() {
        return "BM25(k1=" + k1 + ", b=" + b + ")";
    }
}
