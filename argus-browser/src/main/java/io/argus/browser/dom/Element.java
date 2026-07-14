package io.argus.browser.dom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** An element node: a tag name plus attributes (e.g. {@code <a href="/x" class="link">}). */
public class Element extends Node {

    private final String tagName;
    private final Map<String, String> attributes = new LinkedHashMap<>();

    public Element(String tagName) {
        this.tagName = tagName.toLowerCase(Locale.ROOT);
    }

    public String tagName() {
        return tagName;
    }

    @Override
    public String nodeName() {
        return tagName;
    }

    public void setAttribute(String name, String value) {
        attributes.put(name.toLowerCase(Locale.ROOT), value == null ? "" : value);
    }

    public String getAttribute(String name) {
        return attributes.get(name.toLowerCase(Locale.ROOT));
    }

    public boolean hasAttribute(String name) {
        return attributes.containsKey(name.toLowerCase(Locale.ROOT));
    }

    public Map<String, String> attributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public String id() {
        return attributes.getOrDefault("id", "");
    }

    /** The element's CSS classes (the {@code class} attribute split on whitespace). */
    public List<String> classList() {
        String c = attributes.get("class");
        if (c == null || c.isBlank()) {
            return List.of();
        }
        return Arrays.asList(c.trim().split("\\s+"));
    }

    /** Direct child elements (skipping text and comment nodes). */
    public List<Element> childElements() {
        List<Element> out = new ArrayList<>();
        for (Node c : childNodes()) {
            if (c instanceof Element e) {
                out.add(e);
            }
        }
        return out;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<").append(tagName);
        for (Map.Entry<String, String> a : attributes.entrySet()) {
            sb.append(' ').append(a.getKey()).append("=\"").append(a.getValue()).append('"');
        }
        return sb.append('>').toString();
    }
}
