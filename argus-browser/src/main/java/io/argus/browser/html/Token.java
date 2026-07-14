package io.argus.browser.html;

import java.util.Map;

/** A single lexical token produced by {@link HtmlTokenizer}. */
public sealed interface Token {

    record StartTag(String name, Map<String, String> attributes, boolean selfClosing) implements Token {
    }

    record EndTag(String name) implements Token {
    }

    record Characters(String data) implements Token {
    }

    record Comment(String data) implements Token {
    }

    record Doctype(String name) implements Token {
    }

    record Eof() implements Token {
    }
}
