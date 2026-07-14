package io.argus.browser.html;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.argus.browser.dom.Document;
import io.argus.browser.dom.Element;
import java.util.List;
import org.junit.jupiter.api.Test;

class HtmlParserTest {

    private final HtmlParser parser = new HtmlParser();

    @Test
    void parsesNestedElements() {
        Document doc = parser.parse("<html><body><p>Hello</p></body></html>");
        assertEquals("html", doc.documentElement().tagName());
        assertEquals(1, doc.getElementsByTagName("body").size());
        assertEquals("Hello", doc.getElementsByTagName("p").get(0).textContent());
    }

    @Test
    void parsesQuotedSingleQuotedAndUnquotedAttributes() {
        Document doc = parser.parse("<a href=\"/x\" class='link primary' data-n=5>text</a>");
        Element a = doc.getElementsByTagName("a").get(0);
        assertEquals("/x", a.getAttribute("href"));
        assertEquals(List.of("link", "primary"), a.classList());
        assertEquals("5", a.getAttribute("data-n"));
        assertEquals("text", a.textContent());
    }

    @Test
    void voidElementsDoNotNest() {
        Document doc = parser.parse("<div><br><img src=\"a.png\">after</div>");
        Element div = doc.getElementsByTagName("div").get(0);
        assertEquals(1, doc.getElementsByTagName("br").size());
        assertEquals("a.png", doc.getElementsByTagName("img").get(0).getAttribute("src"));
        assertEquals("after", div.textContent());
    }

    @Test
    void ignoresCommentsAndDoctype() {
        Document doc = parser.parse("<!doctype html><!-- hidden --><p>x</p>");
        assertEquals(1, doc.getElementsByTagName("p").size());
        assertEquals("x", doc.getElementsByTagName("p").get(0).textContent());
    }

    @Test
    void getElementByIdFindsDescendant() {
        Document doc = parser.parse("<div><span id=\"target\">found</span></div>");
        Element el = doc.getElementById("target");
        assertNotNull(el);
        assertEquals("found", el.textContent());
    }

    @Test
    void unescapesEntities() {
        Document doc = parser.parse("<p>a &amp; b &lt; c &gt; d</p>");
        assertEquals("a & b < c > d", doc.getElementsByTagName("p").get(0).textContent());
    }

    @Test
    void handlesSelfClosingAndStrayEndTags() {
        Document doc = parser.parse("<input type=text /><div/>tail</span>");
        assertEquals("text", doc.getElementsByTagName("input").get(0).getAttribute("type"));
        // stray </span> must not throw and tail text survives
        assertEquals(1, doc.getElementsByTagName("div").size());
    }

    @Test
    void toleratesUnterminatedInputWithoutError() {
        Document doc = parser.parse("<ul><li>one<li>two");
        assertEquals(2, doc.getElementsByTagName("li").size());
    }
}
