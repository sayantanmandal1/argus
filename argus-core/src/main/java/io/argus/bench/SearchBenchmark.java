package io.argus.bench;

import io.argus.analysis.StandardAnalyzer;
import io.argus.document.Document;
import io.argus.index.IndexReader;
import io.argus.index.IndexWriter;
import io.argus.query.QueryParser;
import io.argus.search.IndexSearcher;
import io.argus.search.Query;
import java.util.Arrays;
import java.util.Random;

/**
 * A small, dependency-free benchmark for the search engine. It builds a synthetic corpus, measures
 * indexing throughput, then runs a mix of term, boolean, and phrase queries and reports latency
 * percentiles and queries-per-second. It is a rough guide (single JVM, no JMH warmup rigor), not a
 * publication-grade micro-benchmark, but enough to catch regressions and characterize performance.
 *
 * <pre>{@code
 *   java -cp argus-core/target/classes io.argus.bench.SearchBenchmark 50000 20000
 * }</pre>
 */
public final class SearchBenchmark {

    /** Aggregated benchmark output. Latencies are in microseconds. */
    public record Result(
            int docs,
            double indexMillis,
            double docsPerSecond,
            int queries,
            double meanQueryMicros,
            double p50Micros,
            double p95Micros,
            double p99Micros,
            double queriesPerSecond) {
    }

    private final long seed;

    public SearchBenchmark(long seed) {
        this.seed = seed;
    }

    public Result run(int numDocs, int numQueryRuns) {
        Random random = new Random(seed);
        String[] vocabulary = vocabulary(2000);

        IndexWriter writer = new IndexWriter();
        long indexStart = System.nanoTime();
        for (int i = 0; i < numDocs; i++) {
            Document doc = new Document();
            doc.addKeyword("id", "d" + i);
            doc.addText("title", phrase(vocabulary, 4 + random.nextInt(6), random));
            doc.addText("body", phrase(vocabulary, 30 + random.nextInt(60), random));
            writer.addDocument(doc);
        }
        double indexMillis = (System.nanoTime() - indexStart) / 1e6;

        IndexReader reader = writer.getReader();
        IndexSearcher searcher = new IndexSearcher(reader);
        StandardAnalyzer analyzer = new StandardAnalyzer();
        String[] queries = buildQueries(vocabulary, random);

        int warmup = Math.min(numQueryRuns, 1000);
        for (int i = 0; i < warmup; i++) {
            runQuery(searcher, analyzer, queries[i % queries.length]);
        }

        long[] latencies = new long[numQueryRuns];
        long searchStart = System.nanoTime();
        for (int i = 0; i < numQueryRuns; i++) {
            long start = System.nanoTime();
            runQuery(searcher, analyzer, queries[i % queries.length]);
            latencies[i] = System.nanoTime() - start;
        }
        double searchSeconds = (System.nanoTime() - searchStart) / 1e9;

        Arrays.sort(latencies);
        double mean = Arrays.stream(latencies).average().orElse(0) / 1000.0;
        return new Result(
                numDocs,
                indexMillis,
                numDocs / (indexMillis / 1000.0),
                numQueryRuns,
                mean,
                percentile(latencies, 0.50) / 1000.0,
                percentile(latencies, 0.95) / 1000.0,
                percentile(latencies, 0.99) / 1000.0,
                numQueryRuns / searchSeconds);
    }

    private void runQuery(IndexSearcher searcher, StandardAnalyzer analyzer, String queryString) {
        Query query = new QueryParser("body", analyzer).parse(queryString);
        searcher.search(query, 10);
    }

    private String[] buildQueries(String[] vocabulary, Random random) {
        String[] queries = new String[200];
        for (int i = 0; i < queries.length; i++) {
            String a = vocabulary[random.nextInt(vocabulary.length)];
            String b = vocabulary[random.nextInt(vocabulary.length)];
            queries[i] = switch (i % 3) {
                case 0 -> a;
                case 1 -> a + " AND " + b;
                default -> "\"" + a + " " + b + "\"";
            };
        }
        return queries;
    }

    private static String[] vocabulary(int size) {
        String[] words = new String[size];
        for (int i = 0; i < size; i++) {
            words[i] = "term" + Integer.toString(i, 36);
        }
        return words;
    }

    private static String phrase(String[] vocabulary, int length, Random random) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(vocabulary[random.nextInt(vocabulary.length)]);
        }
        return sb.toString();
    }

    private static double percentile(long[] sorted, double p) {
        if (sorted.length == 0) {
            return 0;
        }
        int index = Math.min(sorted.length - 1, (int) Math.round(p * (sorted.length - 1)));
        return sorted[index];
    }

    public static void main(String[] args) {
        int docs = args.length > 0 ? Integer.parseInt(args[0]) : 50_000;
        int queries = args.length > 1 ? Integer.parseInt(args[1]) : 20_000;
        System.out.printf("Indexing %,d documents and running %,d queries\u2026%n", docs, queries);
        Result r = new SearchBenchmark(42).run(docs, queries);
        System.out.println("---------------------------------------------");
        System.out.printf("Indexed        : %,d docs in %.0f ms%n", r.docs(), r.indexMillis());
        System.out.printf("Index throughput: %,.0f docs/sec%n", r.docsPerSecond());
        System.out.printf("Query mix       : %,d runs (term / boolean / phrase)%n", r.queries());
        System.out.printf("Query latency   : mean %.1f us | p50 %.1f | p95 %.1f | p99 %.1f%n",
                r.meanQueryMicros(), r.p50Micros(), r.p95Micros(), r.p99Micros());
        System.out.printf("Query throughput: %,.0f queries/sec%n", r.queriesPerSecond());
        System.out.println("---------------------------------------------");
    }
}
