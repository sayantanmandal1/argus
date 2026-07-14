package io.argus.browser.paint;

import io.argus.browser.layout.TextMeasurer;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * A {@link TextMeasurer} backed by real AWT {@link FontMetrics}, so that inline line-breaking during
 * layout matches what the {@link Painter} actually draws. Works headlessly because it measures
 * against an off-screen image's graphics context.
 */
public final class AwtTextMeasurer implements TextMeasurer {

    private final Graphics2D graphics;

    public AwtTextMeasurer() {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        this.graphics = image.createGraphics();
    }

    @Override
    public double textWidth(String text, double fontSize, boolean bold) {
        graphics.setFont(font(fontSize, bold));
        return graphics.getFontMetrics().stringWidth(text);
    }

    @Override
    public double lineHeight(double fontSize) {
        graphics.setFont(font(fontSize, false));
        return graphics.getFontMetrics().getHeight();
    }

    private static Font font(double fontSize, boolean bold) {
        return new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, (int) Math.round(fontSize));
    }
}
