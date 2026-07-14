package io.argus.browser.js;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.argus.browser.dom.Document;
import io.argus.browser.dom.Element;
import io.argus.browser.html.HtmlParser;
import java.util.List;
import org.junit.jupiter.api.Test;

class DomScriptingTest {

    private static Document parse(String html) {
        return new HtmlParser().parse(html);
    }

    @Test
    void scriptSetsTextContent() {
        Document doc = parse("<html><body><p id=\"x\">old</p></body></html>");
        new JsEngine(doc).run("document.getElementById('x').textContent = 'new';");
        assertEquals("new", doc.getElementById("x").textContent());
    }

    @Test
    void scriptCreatesAndAppendsElement() {
        Document doc = parse("<html><body><div id=\"root\"></div></body></html>");
        new JsEngine(doc).run(
                "var d = document.getElementById('root');"
                        + "var s = document.createElement('span'); s.textContent = 'hi'; d.appendChild(s);");
        Element root = doc.getElementById("root");
        assertEquals(1, root.childElements().size());
        assertEquals("span", root.childElements().get(0).tagName());
        assertEquals("hi", root.textContent());
    }

    @Test
    void querySelectorUsesCssEngine() {
        Document doc = parse("<html><body><p class=\"a\">1</p><p class=\"a\">2</p></body></html>");
        JsEngine engine = new JsEngine(doc);
        engine.run("console.log(document.querySelectorAll('.a').length);"
                + " console.log(document.querySelector('.a').textContent);");
        assertEquals(List.of("2", "1"), engine.consoleOutput());
    }

    @Test
    void styleAssignmentUpdatesInlineAttribute() {
        Document doc = parse("<html><body><div id=\"b\"></div></body></html>");
        new JsEngine(doc).run("document.getElementById('b').style.backgroundColor = 'red';");
        assertTrue(doc.getElementById("b").getAttribute("style").contains("background-color: red"));
    }

    @Test
    void innerHtmlReplacesChildren() {
        Document doc = parse("<html><body><div id=\"c\">old</div></body></html>");
        new JsEngine(doc).run("document.getElementById('c').innerHTML = '<b>hi</b>';");
        Element c = doc.getElementById("c");
        assertEquals(1, c.childElements().size());
        assertEquals("b", c.childElements().get(0).tagName());
        assertEquals("hi", c.textContent());
    }

    @Test
    void loopBuildsAListInTheDom() {
        Document doc = parse("<html><body><ul id=\"list\"></ul></body></html>");
        new JsEngine(doc).run(
                "var ul = document.getElementById('list');"
                        + "for (var i = 1; i <= 3; i++){"
                        + "  var li = document.createElement('li');"
                        + "  li.textContent = 'Item ' + i;"
                        + "  ul.appendChild(li);"
                        + "}");
        Element ul = doc.getElementById("list");
        assertEquals(3, ul.childElements().size());
        assertEquals("Item 2", ul.childElements().get(1).textContent());
    }

    @Test
    void scriptCanReadAttributesAndClassName() {
        Document doc = parse("<html><body><a id=\"link\" href=\"/x\" class=\"nav main\">go</a></body></html>");
        JsEngine engine = new JsEngine(doc);
        engine.run("var a = document.getElementById('link');"
                + " console.log(a.getAttribute('href')); console.log(a.className); console.log(a.tagName);");
        assertEquals(List.of("/x", "nav main", "A"), engine.consoleOutput());
    }
}
