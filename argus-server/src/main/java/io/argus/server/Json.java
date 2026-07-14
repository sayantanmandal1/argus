package io.argus.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A tiny, dependency-free JSON reader/writer — enough for the server's request and response bodies.
 * Objects become {@link LinkedHashMap}, arrays become {@link List}, numbers become {@code Long} or
 * {@code Double}, and strings/booleans/null map to their Java equivalents.
 */
public final class Json {

    private Json() {
    }

    // ---------------------------------------------------------------- writer

    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value);
        return sb.toString();
    }

    private static void writeValue(StringBuilder sb, Object v) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof String s) {
            writeString(sb, s);
        } else if (v instanceof Boolean b) {
            sb.append(b.toString());
        } else if (v instanceof Double || v instanceof Float) {
            double d = ((Number) v).doubleValue();
            sb.append(Double.isNaN(d) || Double.isInfinite(d) ? "null" : v.toString());
        } else if (v instanceof Number n) {
            sb.append(n.toString());
        } else if (v instanceof Map<?, ?> m) {
            writeObject(sb, m);
        } else if (v instanceof Iterable<?> it) {
            writeArray(sb, it);
        } else if (v instanceof Object[] arr) {
            writeArray(sb, Arrays.asList(arr));
        } else {
            writeString(sb, v.toString());
        }
    }

    private static void writeObject(StringBuilder sb, Map<?, ?> m) {
        sb.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> e : m.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            writeString(sb, String.valueOf(e.getKey()));
            sb.append(':');
            writeValue(sb, e.getValue());
        }
        sb.append('}');
    }

    private static void writeArray(StringBuilder sb, Iterable<?> it) {
        sb.append('[');
        boolean first = true;
        for (Object o : it) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            writeValue(sb, o);
        }
        sb.append(']');
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }

    // ---------------------------------------------------------------- parser

    public static Object parse(String text) {
        Parser p = new Parser(text);
        p.skipWhitespace();
        Object v = p.value();
        p.skipWhitespace();
        if (!p.eof()) {
            throw new JsonException("trailing characters at position " + p.pos);
        }
        return v;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        Object v = parse(text);
        if (!(v instanceof Map)) {
            throw new JsonException("expected a JSON object");
        }
        return (Map<String, Object>) v;
    }

    /** Thrown on malformed JSON. */
    public static final class JsonException extends RuntimeException {
        public JsonException(String message) {
            super(message);
        }
    }

    private static final class Parser {
        private final String s;
        private int pos;

        Parser(String s) {
            this.s = s;
        }

        boolean eof() {
            return pos >= s.length();
        }

        char peek() {
            return s.charAt(pos);
        }

        void skipWhitespace() {
            while (!eof()) {
                char c = peek();
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    pos++;
                } else {
                    break;
                }
            }
        }

        Object value() {
            skipWhitespace();
            if (eof()) {
                throw new JsonException("unexpected end of input");
            }
            return switch (peek()) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> string();
                case 't', 'f' -> bool();
                case 'n' -> nul();
                default -> number();
            };
        }

        Map<String, Object> object() {
            expect('{');
            Map<String, Object> m = new LinkedHashMap<>();
            skipWhitespace();
            if (!eof() && peek() == '}') {
                pos++;
                return m;
            }
            while (true) {
                skipWhitespace();
                String key = string();
                skipWhitespace();
                expect(':');
                m.put(key, value());
                skipWhitespace();
                char c = next();
                if (c == '}') {
                    return m;
                }
                if (c != ',') {
                    throw new JsonException("expected ',' or '}' at position " + pos);
                }
            }
        }

        List<Object> array() {
            expect('[');
            List<Object> list = new ArrayList<>();
            skipWhitespace();
            if (!eof() && peek() == ']') {
                pos++;
                return list;
            }
            while (true) {
                list.add(value());
                skipWhitespace();
                char c = next();
                if (c == ']') {
                    return list;
                }
                if (c != ',') {
                    throw new JsonException("expected ',' or ']' at position " + pos);
                }
            }
        }

        String string() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (eof()) {
                    throw new JsonException("unterminated string");
                }
                char c = s.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    char e = s.charAt(pos++);
                    switch (e) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'u' -> {
                            sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                            pos += 4;
                        }
                        default -> throw new JsonException("invalid escape \\" + e);
                    }
                } else {
                    sb.append(c);
                }
            }
        }

        Object number() {
            int start = pos;
            while (!eof()) {
                char c = peek();
                if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
                    pos++;
                } else {
                    break;
                }
            }
            String num = s.substring(start, pos);
            if (num.isEmpty()) {
                throw new JsonException("invalid value at position " + start);
            }
            if (num.contains(".") || num.contains("e") || num.contains("E")) {
                return Double.parseDouble(num);
            }
            try {
                return Long.parseLong(num);
            } catch (NumberFormatException e) {
                return Double.parseDouble(num);
            }
        }

        Object bool() {
            if (s.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            }
            if (s.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            throw new JsonException("invalid literal at position " + pos);
        }

        Object nul() {
            if (s.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            throw new JsonException("invalid literal at position " + pos);
        }

        void expect(char c) {
            if (eof() || s.charAt(pos) != c) {
                throw new JsonException("expected '" + c + "' at position " + pos);
            }
            pos++;
        }

        char next() {
            if (eof()) {
                throw new JsonException("unexpected end of input");
            }
            return s.charAt(pos++);
        }
    }
}
