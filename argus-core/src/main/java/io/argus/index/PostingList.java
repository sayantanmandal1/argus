package io.argus.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The inverted list for a single term in a single field: every {@link Posting} for that term, kept
 * sorted by ascending document id so that boolean and phrase queries can merge-join efficiently.
 */
public final class PostingList {

    private final List<Posting> postings = new ArrayList<>();
    private long totalTermFrequency;

    /** Appends a posting. Postings must be added in strictly increasing document-id order. */
    public void add(Posting posting) {
        if (!postings.isEmpty() && posting.docId() <= postings.get(postings.size() - 1).docId()) {
            throw new IllegalArgumentException("postings must be added in increasing docId order");
        }
        postings.add(posting);
        totalTermFrequency += posting.frequency();
    }

    public List<Posting> postings() {
        return Collections.unmodifiableList(postings);
    }

    /** The number of documents that contain the term. */
    public int docFrequency() {
        return postings.size();
    }

    /** The total number of occurrences of the term across all documents. */
    public long totalTermFrequency() {
        return totalTermFrequency;
    }

    /** Binary-searches for the posting of {@code docId}, or returns {@code null} if absent. */
    public Posting find(int docId) {
        int lo = 0;
        int hi = postings.size() - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int d = postings.get(mid).docId();
            if (d == docId) {
                return postings.get(mid);
            }
            if (d < docId) {
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return null;
    }
}
