package io.argus.bench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SearchBenchmarkTest {

    @Test
    void harnessRunsAndReportsThroughput() {
        SearchBenchmark.Result result = new SearchBenchmark(1).run(500, 200);
        assertEquals(500, result.docs());
        assertTrue(result.docsPerSecond() > 0, "indexing throughput should be positive");
        assertTrue(result.queriesPerSecond() > 0, "query throughput should be positive");
        assertTrue(result.p99Micros() >= result.p50Micros(), "p99 should be >= p50");
    }
}
