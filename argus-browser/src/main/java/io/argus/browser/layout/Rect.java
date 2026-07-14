package io.argus.browser.layout;

/**
 * A rectangle in CSS pixels. Mutable because the layout algorithm fills in position and size in
 * several passes (width during the containing-block pass, position during placement, height after
 * children are laid out).
 */
public final class Rect {

    public double x;
    public double y;
    public double width;
    public double height;

    public Rect(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /** Returns a new rectangle grown on each side by the given edge sizes. */
    public Rect expandedBy(EdgeSizes edge) {
        return new Rect(
                x - edge.left(),
                y - edge.top(),
                width + edge.left() + edge.right(),
                height + edge.top() + edge.bottom());
    }

    @Override
    public String toString() {
        return String.format("Rect[x=%.1f, y=%.1f, w=%.1f, h=%.1f]", x, y, width, height);
    }
}
