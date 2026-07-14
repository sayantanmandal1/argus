package io.argus.browser.layout;

/** The four edge thicknesses (margin, border, or padding) around a box, in CSS pixels. */
public record EdgeSizes(double top, double right, double bottom, double left) {

    public static final EdgeSizes ZERO = new EdgeSizes(0, 0, 0, 0);
}
