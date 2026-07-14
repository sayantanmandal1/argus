package io.argus.browser.css;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A practical CSS parser. It strips comments, then repeatedly reads a selector list up to '{' and a
 * declaration block up to '}'. Supports type, universal, id, class, and compound selectors joined by
 * descendant combinators, plus selector lists. This is a useful subset — not the full CSS grammar.
 */
public final class CssParser {

    public Stylesheet parse(String css) {
        String cleaned = stripComments(css == null ? "" : css);
        List<Rule> rules = new ArrayList<>();
        int i = 0;
        while (i < cleaned.length()) {
            int open = cleaned.indexOf('{', i);
            if (open < 0) {
                break;
            }
            String selectorText = cleaned.substring(i, open).trim();
            int close = cleaned.indexOf('}', open);
            if (close < 0) {
                close = cleaned.length();
            }
            String body = cleaned.substring(open + 1, Math.min(close, cleaned.length()));
            i = close + 1;
            if (selectorText.isEmpty()) {
                continue;
            }
            List<Selector> selectors = parseSelectorList(selectorText);
            List<Declaration> declarations = parseDeclarations(body);
            if (!selectors.isEmpty()) {
                rules.add(new Rule(selectors, declarations));
            }
        }
        return new Stylesheet(rules);
    }

    public List<Selector> parseSelectorList(String text) {
        List<Selector> out = new ArrayList<>();
        for (String part : text.split(",")) {
            Selector selector = parseSelector(part.trim());
            if (selector != null) {
                out.add(selector);
            }
        }
        return out;
    }

    public Selector parseSelector(String text) {
        List<SimpleSelector> sequence = new ArrayList<>();
        for (String token : text.trim().split("\\s+")) {
            if (token.isEmpty()) {
                continue;
            }
            sequence.add(parseSimpleSelector(token));
        }
        return sequence.isEmpty() ? null : new Selector(sequence);
    }

    private SimpleSelector parseSimpleSelector(String token) {
        String tag = null;
        String id = null;
        List<String> classes = new ArrayList<>();

        int i = 0;
        int start = i;
        while (i < token.length() && token.charAt(i) != '.' && token.charAt(i) != '#') {
            i++;
        }
        String lead = token.substring(start, i);
        if (!lead.isEmpty()) {
            tag = lead.toLowerCase(Locale.ROOT);
        }
        while (i < token.length()) {
            char kind = token.charAt(i);
            i++;
            int nameStart = i;
            while (i < token.length() && token.charAt(i) != '.' && token.charAt(i) != '#') {
                i++;
            }
            String name = token.substring(nameStart, i);
            if (name.isEmpty()) {
                continue;
            }
            if (kind == '#') {
                id = name;
            } else if (kind == '.') {
                classes.add(name);
            }
        }
        return new SimpleSelector(tag, id, classes);
    }

    private List<Declaration> parseDeclarations(String body) {
        List<Declaration> out = new ArrayList<>();
        for (String piece : body.split(";")) {
            String declaration = piece.trim();
            if (declaration.isEmpty()) {
                continue;
            }
            int colon = declaration.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String property = declaration.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String value = declaration.substring(colon + 1).trim();
            if (!property.isEmpty()) {
                out.add(new Declaration(property, value));
            }
        }
        return out;
    }

    private static String stripComments(String css) {
        return css.replaceAll("(?s)/\\*.*?\\*/", "");
    }
}
