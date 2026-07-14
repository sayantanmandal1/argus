package io.argus.browser.dom;

/** The root of a parsed document. Its {@link #documentElement()} is the top-level {@code <html>}. */
public final class Document extends Node {

    @Override
    public String nodeName() {
        return "#document";
    }

    /** The document's root element (the first element child), or {@code null} if empty. */
    public Element documentElement() {
        for (Node c : childNodes()) {
            if (c instanceof Element e) {
                return e;
            }
        }
        return null;
    }
}
