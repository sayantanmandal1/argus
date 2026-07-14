package io.argus.index;

import io.argus.document.Document;
import java.util.Collection;

/**
 * Read-only view over an index: term lookups, per-field statistics needed for relevance scoring,
 * and access to stored documents. A {@link io.argus.search.Similarity} plus a query executor build
 * on top of this interface, so the same search code works over an in-memory or on-disk index.
 */
public interface IndexReader {

    /** Total number of document slots, including deleted ones. */
    int maxDoc();

    /** Number of live (non-deleted) documents. */
    int numDocs();

    boolean isDeleted(int docId);

    /** The stored document for {@code docId}, or {@code null} if out of range. */
    Document document(int docId);

    /** The postings for {@code term} in {@code field}, or {@code null} if the term is absent. */
    PostingList postings(String field, String term);

    /** The number of documents containing {@code term} in {@code field}. */
    int docFrequency(String field, String term);

    /** The number of indexed tokens of {@code field} in {@code docId} (for length normalization). */
    int fieldLength(int docId, String field);

    /** The summed token count of {@code field} across all documents. */
    long totalFieldLength(String field);

    /** The number of documents that have at least one token in {@code field}. */
    int docCountWithField(String field);

    /** The mean token count of {@code field} over documents that contain it. */
    double averageFieldLength(String field);

    /** All indexed field names. */
    Collection<String> fields();

    /** All indexed terms in {@code field}. */
    Collection<String> terms(String field);
}
