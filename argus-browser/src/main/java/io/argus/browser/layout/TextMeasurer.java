package io.argus.browser.layout;

/**
 * Estimates text dimensions during inline layout. The engine's layout stage must run headlessly (no
 * AWT), so the default is a font-metric-free heuristic; the paint stage can supply a measurer backed
 * by real {@link java.awt.FontMetrics} for pixel-accurate line breaking.
 */
public interface TextMeasurer {

    double textWidth(String text, double fontSize, boolean bold);

    double lineHeight(double fontSize);

    /** A dependency-free approximation: average glyph width scales with font size. */
    TextMeasurer DEFAULT = new TextMeasurer() {
        @Override
        public double textWidth(String text, double fontSize, boolean bold) {
            return text.length() * fontSize * (bold ? 0.55 : 0.5);
        }

        @Override
        public double lineHeight(double fontSize) {
            return fontSize * 1.2;
        }
    };
}
