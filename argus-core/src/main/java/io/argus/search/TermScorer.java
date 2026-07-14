package io.argus.search;

import io.argus.index.IndexReader;
import io.argus.index.Posting;
import io.argus.index.PostingList;
import java.util.List;

/**
 * Scores a single term by walking its postings in document order, applying the configured
 * {@link Similarity}. Deleted documents are skipped transparently.
 */
public final class TermScorer extends Scorer {

    private final IndexReader reader;
    private final String field;
    private final List<Posting> postings;
    private final Similarity similarity;
    private final double idf;
    private final double avgFieldLength;

    private int cursor = -1;
    private int docId = -1;

    public TermScorer(IndexReader reader, String field, PostingList postingList, Similarity similarity) {
        this.reader = reader;
        this.field = field;
        this.postings = postingList.postings();
        this.similarity = similarity;
        long n = Math.max(reader.numDocs(), 1);
        this.idf = similarity.idf(postingList.docFrequency(), n);
        this.avgFieldLength = reader.averageFieldLength(field);
    }

    public double idf() {
        return idf;
    }

    @Override
    public int docId() {
        return docId;
    }

    @Override
    public int nextDoc() {
        return scanFrom(cursor + 1);
    }

    @Override
    public int advance(int target) {
        int lo = cursor + 1;
        int hi = postings.size() - 1;
        int res = postings.size();
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int d = postings.get(mid).docId();
            if (d >= target) {
                res = mid;
                hi = mid - 1;
            } else {
                lo = mid + 1;
            }
        }
        return scanFrom(res);
    }

    private int scanFrom(int start) {
        int i = start;
        while (i < postings.size()) {
            int d = postings.get(i).docId();
            if (!reader.isDeleted(d)) {
                cursor = i;
                docId = d;
                return d;
            }
            i++;
        }
        cursor = postings.size();
        docId = NO_MORE_DOCS;
        return NO_MORE_DOCS;
    }

    @Override
    public double score() {
        Posting p = postings.get(cursor);
        int fieldLength = reader.fieldLength(docId, field);
        return similarity.score(p.frequency(), idf, fieldLength, avgFieldLength);
    }

    /** Term frequency of the current document. */
    public int freq() {
        return postings.get(cursor).frequency();
    }

    /** Positions of the term in the current document. */
    public int[] positions() {
        return postings.get(cursor).positions();
    }

    @Override
    public long cost() {
        return postings.size();
    }
}
