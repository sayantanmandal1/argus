package io.argus.browser.js;

import io.argus.browser.css.CssParser;
import io.argus.browser.css.Selector;
import io.argus.browser.dom.Document;
import io.argus.browser.dom.Element;
import io.argus.browser.dom.Node;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bridges the Java DOM to the JavaScript engine. It hands out {@link JsObject} wrappers for DOM nodes
 * and caches them by identity so {@code a === a} holds for the same element, and it implements
 * {@code querySelector}/{@code querySelectorAll} by reusing the CSS selector engine.
 */
public final class DomBinding {

    private final Document document;
    private final Map<Node, JsObject> cache = new IdentityHashMap<>();

    public DomBinding(Document document) {
        this.document = document;
        cache.put(document, new DomDocument(this, document));
    }

    public Document document() {
        return document;
    }

    public JsObject documentObject() {
        return cache.get(document);
    }

    /** Returns the wrapper for a node, or {@code null} (JavaScript null) if {@code node} is null. */
    public Object wrap(Node node) {
        if (node == null) {
            return null;
        }
        return cache.computeIfAbsent(node, n -> new DomNode(this, n));
    }

    public JsArray elementArray(List<Element> elements) {
        JsArray array = new JsArray();
        for (Element el : elements) {
            array.items().add(wrap(el));
        }
        return array;
    }

    static Element first(Node root, String selectorText) {
        List<Selector> selectors = parse(selectorText);
        for (Element el : root.getElementsByTagName("*")) {
            if (matchesAny(selectors, el)) {
                return el;
            }
        }
        return null;
    }

    static List<Element> all(Node root, String selectorText) {
        List<Selector> selectors = parse(selectorText);
        List<Element> out = new ArrayList<>();
        for (Element el : root.getElementsByTagName("*")) {
            if (matchesAny(selectors, el)) {
                out.add(el);
            }
        }
        return out;
    }

    private static List<Selector> parse(String selectorText) {
        try {
            return new CssParser().parseSelectorList(selectorText);
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    private static boolean matchesAny(List<Selector> selectors, Element el) {
        for (Selector selector : selectors) {
            if (selector.matches(el)) {
                return true;
            }
        }
        return false;
    }
}
