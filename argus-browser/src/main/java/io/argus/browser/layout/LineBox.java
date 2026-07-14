package io.argus.browser.layout;

import java.util.ArrayList;
import java.util.List;

/** One line of inline content: the fragments placed on it, its top offset, and its height. */
public final class LineBox {

    private final double top;
    private double height;
    private final List<InlineFragment> fragments = new ArrayList<>();

    public LineBox(double top) {
        this.top = top;
    }

    public double top() {
        return top;
    }

    public double height() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public List<InlineFragment> fragments() {
        return fragments;
    }
}
