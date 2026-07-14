package io.argus.browser.html;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A pragmatic HTML tokenizer. It recognizes start tags (with quoted, single-quoted, and unquoted
 * attributes), end tags, text, comments, and doctype. It follows the spirit of the HTML5 tokenizer
 * without its full state machine — enough to turn realistic markup into a token stream.
 */
public final class HtmlTokenizer {

    private final String input;
    private int pos;

    public HtmlTokenizer(String input) {
        this.input = input == null ? "" : input;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (pos < input.length()) {
            if (input.charAt(pos) == '<') {
                if (input.startsWith("<!--", pos)) {
                    tokens.add(comment());
                } else if (regionMatchesIgnoreCase("<!doctype", pos)) {
                    tokens.add(doctype());
                } else if (pos + 1 < input.length() && input.charAt(pos + 1) == '/') {
                    tokens.add(endTag());
                } else if (pos + 1 < input.length() && Character.isLetter(input.charAt(pos + 1))) {
                    Token tag = startTag();
                    tokens.add(tag);
                    if (tag instanceof Token.StartTag start
                            && !start.selfClosing()
                            && isRawTextElement(start.name())) {
                        tokens.add(rawText(start.name()));
                    }
                } else {
                    tokens.add(new Token.Characters("<"));
                    pos++;
                }
            } else {
                tokens.add(characters());
            }
        }
        tokens.add(new Token.Eof());
        return tokens;
    }

    private Token characters() {
        int start = pos;
        while (pos < input.length() && input.charAt(pos) != '<') {
            pos++;
        }
        return new Token.Characters(unescape(input.substring(start, pos)));
    }

    /**
     * Consumes the literal contents of a raw-text element (script/style) up to its matching end tag,
     * without treating {@code <} as markup or decoding entities. Leaves {@code pos} at the {@code <}
     * of the end tag so the main loop tokenizes it normally.
     */
    private Token rawText(String tagName) {
        int start = pos;
        int close = indexOfIgnoreCase("</" + tagName, pos);
        if (close < 0) {
            pos = input.length();
            return new Token.Characters(input.substring(start));
        }
        String data = input.substring(start, close);
        pos = close;
        return new Token.Characters(data);
    }

    private static boolean isRawTextElement(String name) {
        return name.equals("script") || name.equals("style");
    }

    private int indexOfIgnoreCase(String needle, int from) {
        int limit = input.length() - needle.length();
        for (int i = from; i <= limit; i++) {
            if (input.regionMatches(true, i, needle, 0, needle.length())) {
                return i;
            }
        }
        return -1;
    }

    private Token comment() {
        pos += 4; // consume <!--
        int start = pos;
        int end = input.indexOf("-->", pos);
        if (end < 0) {
            pos = input.length();
            return new Token.Comment(input.substring(start));
        }
        String data = input.substring(start, end);
        pos = end + 3;
        return new Token.Comment(data);
    }

    private Token doctype() {
        int end = input.indexOf('>', pos);
        String content = end < 0 ? input.substring(pos) : input.substring(pos, end);
        pos = end < 0 ? input.length() : end + 1;
        String name = content.replaceAll("(?i)<!doctype", "").trim();
        return new Token.Doctype(name);
    }

    private Token endTag() {
        pos += 2; // consume </
        int start = pos;
        while (pos < input.length() && input.charAt(pos) != '>' && !Character.isWhitespace(input.charAt(pos))) {
            pos++;
        }
        String name = input.substring(start, pos).toLowerCase(Locale.ROOT);
        while (pos < input.length() && input.charAt(pos) != '>') {
            pos++;
        }
        if (pos < input.length()) {
            pos++; // consume >
        }
        return new Token.EndTag(name);
    }

    private Token startTag() {
        pos++; // consume <
        int start = pos;
        while (pos < input.length() && isNameChar(input.charAt(pos))) {
            pos++;
        }
        String name = input.substring(start, pos).toLowerCase(Locale.ROOT);
        Map<String, String> attributes = new LinkedHashMap<>();
        boolean selfClosing = false;

        while (pos < input.length()) {
            skipWhitespace();
            if (pos >= input.length()) {
                break;
            }
            char c = input.charAt(pos);
            if (c == '>') {
                pos++;
                break;
            }
            if (c == '/') {
                selfClosing = true;
                pos++;
                if (pos < input.length() && input.charAt(pos) == '>') {
                    pos++;
                }
                break;
            }
            int nameStart = pos;
            while (pos < input.length() && isAttrNameChar(input.charAt(pos))) {
                pos++;
            }
            if (pos == nameStart) {
                pos++; // avoid stalling on an unexpected character
                continue;
            }
            String attrName = input.substring(nameStart, pos).toLowerCase(Locale.ROOT);
            skipWhitespace();
            String value = "";
            if (pos < input.length() && input.charAt(pos) == '=') {
                pos++;
                skipWhitespace();
                value = attributeValue();
            }
            attributes.put(attrName, value);
        }
        return new Token.StartTag(name, attributes, selfClosing);
    }

    private String attributeValue() {
        if (pos >= input.length()) {
            return "";
        }
        char quote = input.charAt(pos);
        if (quote == '"' || quote == '\'') {
            pos++;
            int start = pos;
            while (pos < input.length() && input.charAt(pos) != quote) {
                pos++;
            }
            String value = input.substring(start, pos);
            if (pos < input.length()) {
                pos++; // consume closing quote
            }
            return unescape(value);
        }
        int start = pos;
        while (pos < input.length()
                && !Character.isWhitespace(input.charAt(pos))
                && input.charAt(pos) != '>'
                && input.charAt(pos) != '/') {
            pos++;
        }
        return unescape(input.substring(start, pos));
    }

    private void skipWhitespace() {
        while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
            pos++;
        }
    }

    private static boolean isNameChar(char c) {
        return Character.isLetterOrDigit(c) || c == '-' || c == ':';
    }

    private static boolean isAttrNameChar(char c) {
        return !Character.isWhitespace(c) && c != '=' && c != '>' && c != '/' && c != '<';
    }

    private boolean regionMatchesIgnoreCase(String s, int at) {
        return input.regionMatches(true, at, s, 0, s.length());
    }

    private static String unescape(String s) {
        if (s.indexOf('&') < 0) {
            return s;
        }
        return s.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&");
    }
}
