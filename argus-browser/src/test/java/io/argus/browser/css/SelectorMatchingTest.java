package io.argus.browser.css;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.argus.browser.dom.Document;
import io.argus.browser.dom.Element;
import io.argus.browser.html.HtmlParser;
import org.junit.jupiter.api.Test;

class SelectorMatchingTest {

    private final CssParser css = new CssParser();

    private Document dom(String html) {
        return new HtmlParser().parse(html);
    }

    @Test
    void typeClassIdAndCompoundSelectorsMatch() {
        Document d = dom("<div><p class=\"note\" id=\"x\">A</p></div><p>B</p>");
        Element a = d.getElementById("x");
        assertTrue(css.parseSelector("p").matches(a));
        assertTrue(css.parseSelector(".note").matches(a));
        assertTrue(css.parseSelector("#x").matches(a));
        assertTrue(css.parseSelector("p.note").matches(a));
        assertFalse(css.parseSelector(".missing").matches(a));
        assertFalse(css.parseSelector("span").matches(a));
    }

    @Test
    void descendantCombinatorConsidersAncestors() {
        Document d = dom("<div><section><p id=\"inside\">A</p></section></div><p id=\"outside\">B</p>");
        Element inside = d.getElementById("inside");
        Element outside = d.getElementById("outside");
        assertTrue(css.parseSelector("div p").matches(inside));
        assertTrue(css.parseSelector("div section p").matches(inside));
        assertFalse(css.parseSelector("div p").matches(outside));
    }

    @Test
    void universalSelectorMatchesEverything() {
        Document d = dom("<p>x</p>");
        assertTrue(css.parseSelector("*").matches(d.getElementsByTagName("p").get(0)));
    }
}
