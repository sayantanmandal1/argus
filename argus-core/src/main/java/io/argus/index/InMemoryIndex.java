package io.argus.index;

import io.argus.document.Document;
import io.argus.util.IntArrayList;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An in-memory inverted index: the authoritative data structure the engine builds and queries.
 * Maps {@code field -> term -> }{@link PostingList}, retains stored documents for retrieval, tracks
 * per-field length statistics for BM25 normalization, and supports soft deletes via a bitset.
 *
 * <p>Mutating methods are package-private and driven by {@link IndexWriter}; the public surface is
 * the read-only {@link IndexReader}.
 */
public final class InMemoryIndex implements IndexReader {

    private final Map<String, Map<String, PostingList>> fieldTerms = new LinkedHashMap<>();
    private final List<Document> storedDocs = new ArrayList<>();
    private final Map<String, IntArrayList> fieldLengths = new LinkedHashMap<>();
    private final Map<String, Long> totalFieldLength = new LinkedHashMap<>();
    private final Map<String, Integer> docCountWithField = new LinkedHashMap<>();
    private final BitSet deleted = new BitSet();
    private int numDeleted;

    // ----------------------------------------------------------------- mutators (IndexWriter)

    int newDocId(Document stored) {
        storedDocs.add(stored);
        return storedDocs.size() - 1;
    }

    void addPosting(String field, String term, Posting posting) {
        fieldTerms.computeIfAbsent(field, f -> new LinkedHashMap<>())
                .computeIfAbsent(term, t -> new PostingList())
                .add(posting);
    }

    void recordFieldLength(int docId, String field, int length) {
        fieldLengths.computeIfAbsent(field, f -> new IntArrayList()).set(docId, length);
        totalFieldLength.merge(field, (long) length, Long::sum);
        docCountWithField.merge(field, 1, Integer::sum);
    }

    void markDeleted(int docId) {
        if (docId >= 0 && docId < storedDocs.size() && !deleted.get(docId)) {
            deleted.set(docId);
            numDeleted++;
        }
    }

    // ----------------------------------------------------------------- IndexReader

    @Override
    public int maxDoc() {
        return storedDocs.size();
    }

    @Override
    public int numDocs() {
        return storedDocs.size() - numDeleted;
    }

    @Override
    public boolean isDeleted(int docId) {
        return deleted.get(docId);
    }

    @Override
    public Document document(int docId) {
        return (docId >= 0 && docId < storedDocs.size()) ? storedDocs.get(docId) : null;
    }

    @Override
    public PostingList postings(String field, String term) {
        Map<String, PostingList> terms = fieldTerms.get(field);
        return terms == null ? null : terms.get(term);
    }

    @Override
    public int docFrequency(String field, String term) {
        PostingList pl = postings(field, term);
        return pl == null ? 0 : pl.docFrequency();
    }

    @Override
    public int fieldLength(int docId, String field) {
        IntArrayList lengths = fieldLengths.get(field);
        if (lengths == null || docId < 0 || docId >= lengths.size()) {
            return 0;
        }
        return lengths.get(docId);
    }

    @Override
    public long totalFieldLength(String field) {
        return totalFieldLength.getOrDefault(field, 0L);
    }

    @Override
    public int docCountWithField(String field) {
        return docCountWithField.getOrDefault(field, 0);
    }

    @Override
    public double averageFieldLength(String field) {
        int dc = docCountWithField(field);
        return dc == 0 ? 0.0 : (double) totalFieldLength(field) / dc;
    }

    @Override
    public Collection<String> fields() {
        return Collections.unmodifiableCollection(fieldTerms.keySet());
    }

    @Override
    public Collection<String> terms(String field) {
        Map<String, PostingList> terms = fieldTerms.get(field);
        return terms == null ? List.of() : Collections.unmodifiableCollection(terms.keySet());
    }
}
