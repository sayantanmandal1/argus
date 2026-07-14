package io.argus.browser.dom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Base class for DOM nodes: a tree of parent/child relationships plus the queries the rest of the
 * engine (styling, layout, scripting) needs. This is a pragmatic subset of the W3C DOM — enough to
 * represent parsed HTML and let a script mutate it.
 */
public abstract class Node {

    private Node parent;
    private final List<Node> children = new ArrayList<>();

    public Node parent() {
        return parent;
    }

    public List<Node> childNodes() {
        return Collections.unmodifiableList(children);
    }

    /** Appends {@code child}, detaching it from any previous parent first. */
    public void appendChild(Node child) {
        if (child.parent != null) {
            child.parent.children.remove(child);
        }
        child.parent = this;
        children.add(child);
    }

    /** Removes {@code child} if it belongs to this node. */
    public void removeChild(Node child) {
        if (children.remove(child)) {
            child.parent = null;
        }
    }

    protected List<Node> mutableChildren() {
        return children;
    }

    public abstract String nodeName();

    /** The concatenated text of this subtree. */
    public String textContent() {
        StringBuilder sb = new StringBuilder();
        collectText(sb);
        return sb.toString();
    }

    void collectText(StringBuilder sb) {
        for (Node c : children) {
            c.collectText(sb);
        }
    }

    /** All descendant elements with the given tag name ({@code "*"} matches all). */
    public List<Element> getElementsByTagName(String name) {
        List<Element> out = new ArrayList<>();
        collectByTag(name.toLowerCase(Locale.ROOT), out);
        return out;
    }

    private void collectByTag(String name, List<Element> out) {
        for (Node c : children) {
            if (c instanceof Element e && (name.equals("*") || e.tagName().equals(name))) {
                out.add(e);
            }
            c.collectByTag(name, out);
        }
    }

    /** The first descendant element whose {@code id} attribute equals {@code id}, or {@code null}. */
    public Element getElementById(String id) {
        for (Node c : children) {
            if (c instanceof Element e) {
                if (id.equals(e.getAttribute("id"))) {
                    return e;
                }
                Element found = e.getElementById(id);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
