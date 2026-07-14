package io.argus.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Verifies the Porter stemmer against canonical word/stem pairs drawn from Porter's original paper
 * and reference vocabulary. If these pass, the algorithm is faithfully implemented.
 */
class PorterStemmerTest {

    private final PorterStemmer stemmer = new PorterStemmer();

    private void assertStem(String word, String expected) {
        assertEquals(expected, stemmer.stem(word), "stem(" + word + ")");
    }

    @Test
    void step1aPlurals() {
        assertStem("caresses", "caress");
        assertStem("ponies", "poni");
        assertStem("ties", "ti");
        assertStem("caress", "caress");
        assertStem("cats", "cat");
    }

    @Test
    void step1bPastAndProgressive() {
        assertStem("feed", "feed");
        assertStem("agreed", "agre");
        assertStem("plastered", "plaster");
        assertStem("bled", "bled");
        assertStem("motoring", "motor");
        assertStem("sing", "sing");
        assertStem("conflated", "conflat");
        assertStem("troubled", "troubl");
        assertStem("sized", "size");
        assertStem("hopping", "hop");
        assertStem("falling", "fall");
        assertStem("hissing", "hiss");
        assertStem("filing", "file");
    }

    @Test
    void step1cYtoI() {
        assertStem("happy", "happi");
        assertStem("sky", "sky");
    }

    @Test
    void step2DoubleSuffixes() {
        assertStem("relational", "relat");
        assertStem("conditional", "condit");
        assertStem("valenci", "valenc");
        assertStem("digitizer", "digit");
        assertStem("conformabli", "conform");
        assertStem("radicalli", "radic");
        assertStem("differentli", "differ");
        assertStem("analogousli", "analog");
        assertStem("vietnamization", "vietnam");
        assertStem("predication", "predic");
        assertStem("operator", "oper");
        assertStem("feudalism", "feudal");
        assertStem("decisiveness", "decis");
        assertStem("hopefulness", "hope");
        assertStem("callousness", "callous");
        assertStem("formaliti", "formal");
        assertStem("sensitiviti", "sensit");
        assertStem("sensibiliti", "sensibl");
    }

    @Test
    void step3And4Suffixes() {
        assertStem("triplicate", "triplic");
        assertStem("formative", "form");
        assertStem("formalize", "formal");
        assertStem("electriciti", "electr");
        assertStem("electrical", "electr");
        assertStem("hopeful", "hope");
        assertStem("goodness", "good");
        assertStem("revival", "reviv");
        assertStem("allowance", "allow");
        assertStem("inference", "infer");
        assertStem("gyroscopic", "gyroscop");
        assertStem("adjustable", "adjust");
        assertStem("defensible", "defens");
        assertStem("irritant", "irrit");
        assertStem("replacement", "replac");
        assertStem("adjustment", "adjust");
        assertStem("dependent", "depend");
        assertStem("adoption", "adopt");
        assertStem("communism", "commun");
        assertStem("activate", "activ");
        assertStem("effective", "effect");
    }

    @Test
    void step5FinalEAndDoubleL() {
        assertStem("probate", "probat");
        assertStem("rate", "rate");
        assertStem("cease", "ceas");
        assertStem("controll", "control");
        assertStem("roll", "roll");
    }

    @Test
    void inflectedFamilyCollapsesToSameStem() {
        String s = stemmer.stem("connect");
        assertEquals(s, stemmer.stem("connected"));
        assertEquals(s, stemmer.stem("connecting"));
        assertEquals(s, stemmer.stem("connection"));
        assertEquals(s, stemmer.stem("connections"));
    }

    @Test
    void shortWordsUnchanged() {
        assertStem("a", "a");
        assertStem("is", "is");
        assertStem("be", "be");
    }
}
