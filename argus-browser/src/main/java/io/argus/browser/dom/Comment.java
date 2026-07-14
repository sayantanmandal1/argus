package io.argus.browser.dom;

/** A comment node ({@code <!-- ... -->}); ignored by layout and rendering. */
public final class Comment extends Node {

    private final String data;

    public Comment(String data) {
        this.data = data == null ? "" : data;
    }

    public String data() {
        return data;
    }

    @Override
    public String nodeName() {
        return "#comment";
    }

    @Override
    void collectText(StringBuilder sb) {
        // comments contribute no text
    }
}
