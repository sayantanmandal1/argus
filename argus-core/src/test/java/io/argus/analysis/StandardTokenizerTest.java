package io.argus.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class StandardTokenizerTest {

    private List<Token> tokens(String text) {
        StandardTokenizer t = new StandardTokenizer(text);
        t.reset();
        java.util.ArrayList<Token> out = new java.util.ArrayList<>();
        while (t.incrementToken()) {
            out.add(t.token().copy());
        }
        return out;
    }

    @Test
    void splitsOnPunctuationAndWhitespace() {
        List<Token> ts = tokens("Hello, world! foo_bar 42");
        assertEquals(List.of("Hello", "world", "foo", "bar", "42"),
                ts.stream().map(Token::term).toList());
    }

    @Test
    void recordsCharacterOffsets() {
        List<Token> ts = tokens("ab cd");
        assertEquals(0, ts.get(0).startOffset());
        assertEquals(2, ts.get(0).endOffset());
        assertEquals(3, ts.get(1).startOffset());
        assertEquals(5, ts.get(1).endOffset());
    }

    @Test
    void emptyAndBlankProduceNoTokens() {
        assertTrue(tokens("").isEmpty());
        assertTrue(tokens("   \t\n ").isEmpty());
    }
}
