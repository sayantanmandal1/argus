package io.argus.browser.js;

import io.argus.browser.dom.Element;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The {@code element.style} object. It reads and writes individual inline style properties by
 * parsing and rewriting the element's {@code style} attribute, translating JavaScript camelCase
 * names ({@code backgroundColor}) to CSS kebab-case ({@code background-color}).
 */
public final class DomStyle extends JsObject {

    private final Element element;

    DomStyle(Element element) {
        this.element = element;
    }

    @Override
    public Object get(String name) {
        if (element == null) {
            return "";
        }
        if (name.equals("cssText")) {
            return orEmpty(element.getAttribute("style"));
        }
        String value = declarations().get(cssName(name));
        return value == null ? "" : value;
    }

    @Override
    public void set(String name, Object value) {
        if (element == null) {
            return;
        }
        String text = JsValues.stringify(value);
        if (name.equals("cssText")) {
            element.setAttribute("style", text);
            return;
        }
        Map<String, String> declarations = declarations();
        String property = cssName(name);
        if (text.isEmpty()) {
            declarations.remove(property);
        } else {
            declarations.put(property, text);
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : declarations.entrySet()) {
            sb.append(e.getKey()).append(": ").append(e.getValue()).append("; ");
        }
        element.setAttribute("style", sb.toString().trim());
    }

    private Map<String, String> declarations() {
        Map<String, String> map = new LinkedHashMap<>();
        String style = element.getAttribute("style");
        if (style != null) {
            for (String part : style.split(";")) {
                int colon = part.indexOf(':');
                if (colon > 0) {
                    map.put(part.substring(0, colon).trim().toLowerCase(Locale.ROOT), part.substring(colon + 1).trim());
                }
            }
        }
        return map;
    }

    private static String cssName(String name) {
        StringBuilder sb = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (Character.isUpperCase(c)) {
                sb.append('-').append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String orEmpty(String s) {
        return s == null ? "" : s;
    }
}
