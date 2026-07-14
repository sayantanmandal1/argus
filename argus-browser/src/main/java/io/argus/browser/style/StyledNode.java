package io.argus.browser.style;

import io.argus.browser.dom.Node;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A DOM node paired with its fully-resolved (cascaded + inherited) property values, plus the styled
 * children. This is the "render tree" input the layout stage walks. Convenience accessors return
 * typed values with sensible defaults so callers do not repeatedly pattern-match {@link Value}.
 */
public final class StyledNode {

    private final Node node;
    private final Map<String, Value> values;
    private final List<StyledNode> children;

    public StyledNode(Node node, Map<String, Value> values, List<StyledNode> children) {
        this.node = node;
        this.values = new LinkedHashMap<>(values);
        this.children = List.copyOf(children);
    }

    public Node node() {
        return node;
    }

    public Map<String, Value> values() {
        return Map.copyOf(values);
    }

    public List<StyledNode> children() {
        return children;
    }

    public Value value(String name) {
        return values.get(name);
    }

    /** The {@code display} keyword, defaulting to {@code inline} as CSS does for unknown elements. */
    public String display() {
        return keyword("display", "inline");
    }

    /** The value of a keyword property, or {@code def} if absent or not a keyword. */
    public String keyword(String name, String def) {
        return (values.get(name) instanceof Value.Keyword k) ? k.name() : def;
    }

    /** The value of a length property resolved to pixels, or {@code def} if absent or not a length. */
    public double length(String name, double def) {
        return (values.get(name) instanceof Value.Length l) ? l.resolve(16.0) : def;
    }

    /** The value of a color property, or {@code null} if absent or not a color. */
    public Color color(String name) {
        return (values.get(name) instanceof Value.ColorValue c) ? c.color() : null;
    }
}
