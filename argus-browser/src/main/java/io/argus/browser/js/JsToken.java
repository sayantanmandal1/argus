package io.argus.browser.js;

/** A lexical token produced by the {@link Lexer}. For strings, {@code text} is the decoded value. */
public record JsToken(JsToken.Type type, String text, int position) {

    public enum Type {
        NUMBER,
        STRING,
        IDENTIFIER,
        KEYWORD,
        PUNCTUATOR,
        EOF
    }

    public boolean is(Type t, String s) {
        return type == t && text.equals(s);
    }

    @Override
    public String toString() {
        return type + "(" + text + ")";
    }
}
