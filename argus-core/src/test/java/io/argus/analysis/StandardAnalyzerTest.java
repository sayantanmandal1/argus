package io.argus.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class StandardAnalyzerTest {

    private final Analyzer analyzer = new StandardAnalyzer();

    @Test
    void removesStopWords() {
        assertTrue(analyzer.terms("body", "the a of and").isEmpty());
    }

    @Test
    void lowercasesAndStemsInflectedForms() {
        assertEquals(List.of("jump", "jump", "jump"), analyzer.terms("body", "jumping JUMPS Jumped"));
    }

    @Test
    void stopWordRemovalLeavesPositionGap() {
        List<Token> tokens = analyzer.analyze("body", "quick the fox");
        assertEquals(2, tokens.size());
        assertEquals(1, tokens.get(0).positionIncrement());
        assertEquals(2, tokens.get(1).positionIncrement()); // +1 for the removed stop word
    }

    @Test
    void queryAndIndexAnalysisAgree() {
        // The same analyzer must map different surface forms to the same index term.
        assertEquals(analyzer.terms("body", "optimize"), analyzer.terms("body", "optimizing"));
    }
}
