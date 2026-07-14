package io.argus.search;

import io.argus.document.Document;
import io.argus.index.IndexReader;
import java.util.Objects;

/**
 * The entry point for querying an index. Wraps an {@link IndexReader} and a {@link Similarity},
 * executes a {@link Query} by driving its {@link Scorer}, and collects the top hits.
 */
public final class IndexSearcher {

    private final IndexReader reader;
    private final Similarity similarity;

    public IndexSearcher(IndexReader reader) {
        this(reader, new BM25Similarity());
    }

    public IndexSearcher(IndexReader reader, Similarity similarity) {
        this.reader = Objects.requireNonNull(reader, "reader");
        this.similarity = Objects.requireNonNull(similarity, "similarity");
    }

    public IndexReader reader() {
        return reader;
    }

    public Similarity similarity() {
        return similarity;
    }

    /** Runs {@code query} and returns up to {@code k} hits ranked by score. */
    public TopDocs search(Query query, int k) {
        Objects.requireNonNull(query, "query");
        Scorer scorer = query.createScorer(reader, similarity);
        TopScoreDocCollector collector = new TopScoreDocCollector(Math.max(1, k));
        if (scorer != null) {
            for (int doc = scorer.nextDoc(); doc != Scorer.NO_MORE_DOCS; doc = scorer.nextDoc()) {
                collector.collect(doc, scorer.score());
            }
        }
        return collector.topDocs();
    }

    /** Retrieves the stored document for a hit. */
    public Document doc(int docId) {
        return reader.document(docId);
    }
}
