package io.argus.browser.js;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A hand-written lexer for a practical subset of JavaScript. It recognizes numbers, single- and
 * double-quoted strings (with the common escape sequences), identifiers and keywords, line and block
 * comments, and the operators and punctuators the parser needs. Regular-expression literals are not
 * supported, so {@code /} is always division or a comment.
 */
public final class Lexer {

    private static final Set<String> KEYWORDS = Set.of(
            "var", "let", "const", "function", "return", "if", "else", "while", "for", "do",
            "true", "false", "null", "undefined", "new", "typeof", "break", "continue", "this",
            "in", "of");

    // Multi-character operators, longest first so the matcher is greedy.
    private static final String[] OPERATORS = {
            "===", "!==", "==", "!=", "<=", ">=", "&&", "||", "++", "--",
            "+=", "-=", "*=", "/=", "%=", "=>",
            "+", "-", "*", "/", "%", "=", "<", ">", "!", "(", ")", "{", "}",
            "[", "]", ";", ",", ".", ":", "?", "&", "|"
    };

    private final String src;
    private int pos;

    public Lexer(String source) {
        this.src = source == null ? "" : source;
    }

    public List<JsToken> tokenize() {
        List<JsToken> tokens = new ArrayList<>();
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (Character.isWhitespace(c)) {
                pos++;
                continue;
            }
            if (c == '/' && peek(1) == '/') {
                lineComment();
                continue;
            }
            if (c == '/' && peek(1) == '*') {
                blockComment();
                continue;
            }
            if (Character.isDigit(c) || (c == '.' && Character.isDigit(peek(1)))) {
                tokens.add(number());
            } else if (c == '"' || c == '\'') {
                tokens.add(string());
            } else if (isIdentifierStart(c)) {
                tokens.add(identifier());
            } else {
                tokens.add(operator());
            }
        }
        tokens.add(new JsToken(JsToken.Type.EOF, "", pos));
        return tokens;
    }

    private void lineComment() {
        while (pos < src.length() && src.charAt(pos) != '\n') {
            pos++;
        }
    }

    private void blockComment() {
        pos += 2;
        int end = src.indexOf("*/", pos);
        pos = end < 0 ? src.length() : end + 2;
    }

    private JsToken number() {
        int start = pos;
        while (pos < src.length() && Character.isDigit(src.charAt(pos))) {
            pos++;
        }
        if (pos < src.length() && src.charAt(pos) == '.') {
            pos++;
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) {
                pos++;
            }
        }
        if (pos < src.length() && (src.charAt(pos) == 'e' || src.charAt(pos) == 'E')) {
            pos++;
            if (pos < src.length() && (src.charAt(pos) == '+' || src.charAt(pos) == '-')) {
                pos++;
            }
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) {
                pos++;
            }
        }
        return new JsToken(JsToken.Type.NUMBER, src.substring(start, pos), start);
    }

    private JsToken string() {
        int start = pos;
        char quote = src.charAt(pos);
        pos++;
        StringBuilder sb = new StringBuilder();
        while (pos < src.length() && src.charAt(pos) != quote) {
            char c = src.charAt(pos);
            if (c == '\\' && pos + 1 < src.length()) {
                pos++;
                sb.append(unescape(src.charAt(pos)));
            } else {
                sb.append(c);
            }
            pos++;
        }
        if (pos < src.length()) {
            pos++; // closing quote
        }
        return new JsToken(JsToken.Type.STRING, sb.toString(), start);
    }

    private static char unescape(char c) {
        return switch (c) {
            case 'n' -> '\n';
            case 't' -> '\t';
            case 'r' -> '\r';
            case 'b' -> '\b';
            case 'f' -> '\f';
            case '0' -> '\0';
            default -> c;
        };
    }

    private JsToken identifier() {
        int start = pos;
        while (pos < src.length() && isIdentifierPart(src.charAt(pos))) {
            pos++;
        }
        String text = src.substring(start, pos);
        JsToken.Type type = KEYWORDS.contains(text) ? JsToken.Type.KEYWORD : JsToken.Type.IDENTIFIER;
        return new JsToken(type, text, start);
    }

    private JsToken operator() {
        for (String op : OPERATORS) {
            if (src.startsWith(op, pos)) {
                int start = pos;
                pos += op.length();
                return new JsToken(JsToken.Type.PUNCTUATOR, op, start);
            }
        }
        // Unknown character: emit it as a single-character punctuator to avoid stalling.
        int start = pos;
        pos++;
        return new JsToken(JsToken.Type.PUNCTUATOR, String.valueOf(src.charAt(start)), start);
    }

    private char peek(int ahead) {
        int i = pos + ahead;
        return i < src.length() ? src.charAt(i) : '\0';
    }

    private static boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_' || c == '$';
    }

    private static boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }
}
