package io.argus.browser.paint;

import io.argus.browser.style.Color;

/**
 * A single primitive in a display list. Painting a page reduces to producing a flat, ordered list of
 * these (filled rectangles for backgrounds and borders, glyph runs for text) and then replaying them
 * onto a graphics surface. Keeping paint commands separate from the drawing backend lets the display
 * list be built and asserted on headlessly.
 */
public sealed interface DisplayCommand permits DisplayCommand.SolidRect, DisplayCommand.Glyphs {

    record SolidRect(double x, double y, double width, double height, Color color) implements DisplayCommand {
    }

    record Glyphs(String text, double x, double y, double fontSize, boolean bold, Color color)
            implements DisplayCommand {
    }
}
