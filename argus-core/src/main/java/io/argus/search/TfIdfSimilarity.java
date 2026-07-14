package io.argus.search;

/**
 * Classic tf-idf with cosine-style length normalization. Retained as an alternative to
 * {@link BM25Similarity} to demonstrate that the ranking model is pluggable.
 *
 * <pre>
 *   idf(t) = 1 + ln(N / (df + 1))
 *   score  = sqrt(tf) * idf^2 / sqrt(|d|)
 * </pre>
 */
public final class TfIdfSimilarity extends Similarity {

    @Override
    public double idf(long docFreq, long docCount) {
        return 1.0 + Math.log((double) docCount / (docFreq + 1));
    }

    @Override
    public double score(int termFreq, double idf, int fieldLength, double avgFieldLength) {
        double tf = Math.sqrt(termFreq);
        double lengthNorm = fieldLength > 0 ? 1.0 / Math.sqrt(fieldLength) : 1.0;
        return tf * idf * idf * lengthNorm;
    }

    @Override
    public String toString() {
        return "TfIdf";
    }
}
