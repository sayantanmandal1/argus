package io.argus.browser.layout;

/**
 * The CSS box model for a single box: the content rectangle plus the surrounding padding, border,
 * and margin edges. The {@code *Box()} helpers expand the content rectangle outward, which is how
 * the algorithm converts between content size and the space a box occupies in its parent.
 */
public final class Dimensions {

    public Rect content = new Rect(0, 0, 0, 0);
    public EdgeSizes padding = EdgeSizes.ZERO;
    public EdgeSizes border = EdgeSizes.ZERO;
    public EdgeSizes margin = EdgeSizes.ZERO;

    public Rect paddingBox() {
        return content.expandedBy(padding);
    }

    public Rect borderBox() {
        return paddingBox().expandedBy(border);
    }

    public Rect marginBox() {
        return borderBox().expandedBy(margin);
    }
}
