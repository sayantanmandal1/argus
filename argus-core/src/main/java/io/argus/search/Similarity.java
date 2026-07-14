package io.argus.search;

/**
 * Pluggable relevance model. Given corpus and per-document statistics, produces the score
 * contribution of a term match. Implementations include {@link BM25Similarity} (the default) and
 * {@link TfIdfSimilarity}.
 */
public abstract class Similarity {

    /**
     * Inverse document frequency: how much weight a term carries given how many documents contain it.
     *
     * @param docFreq number of documents containing the term
     * @param docCount total number of live documents
     */
    public abstract double idf(long docFreq, long docCount);

    /**
     * Score of a single term's occurrences within one document's field.
     *
     * @param termFreq        occurrences of the term in the document
     * @param idf             value from {@link #idf(long, long)}
     * @param fieldLength     token count of the field in this document
     * @param avgFieldLength  mean token count of the field across the corpus
     */
    public abstract double score(int termFreq, double idf, int fieldLength, double avgFieldLength);
}
