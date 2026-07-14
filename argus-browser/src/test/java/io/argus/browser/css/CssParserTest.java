package io.argus.browser.css;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CssParserTest {

    private final CssParser parser = new CssParser();

    @Test
    void parsesRuleWithDeclarations() {
        Stylesheet s = parser.parse("h1 { color: red; font-size: 20px; }");
        assertEquals(1, s.rules().size());
        Rule r = s.rules().get(0);
        assertEquals("h1", r.selectors().get(0).toString());
        assertEquals(2, r.declarations().size());
        assertEquals("color", r.declarations().get(0).property());
        assertEquals("red", r.declarations().get(0).value());
        assertEquals("20px", r.declarations().get(1).value());
    }

    @Test
    void parsesSelectorList() {
        Stylesheet s = parser.parse("h1, .title, #main { color: blue }");
        assertEquals(1, s.rules().size());
        assertEquals(3, s.rules().get(0).selectors().size());
    }

    @Test
    void stripsComments() {
        Stylesheet s = parser.parse("/* header */ p { margin: 0 } /* trailing */");
        assertEquals(1, s.rules().size());
        assertEquals("p", s.rules().get(0).selectors().get(0).toString());
    }

    @Test
    void parsesMultipleRules() {
        Stylesheet s = parser.parse("a { color: red } b { color: green } c { color: blue }");
        assertEquals(3, s.rules().size());
    }

    @Test
    void specificityOrdersIdOverClassOverType() {
        Selector id = parser.parseSelector("#main");
        Selector cls = parser.parseSelector(".note");
        Selector type = parser.parseSelector("div");
        assertTrue(id.specificity().compareTo(cls.specificity()) > 0);
        assertTrue(cls.specificity().compareTo(type.specificity()) > 0);
    }

    @Test
    void compoundSelectorAccumulatesSpecificity() {
        Selector s = parser.parseSelector("div.note#main");
        assertEquals(new Specificity(1, 1, 1), s.specificity());
    }
}
