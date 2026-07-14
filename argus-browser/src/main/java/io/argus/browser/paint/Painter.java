package io.argus.browser.paint;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Replays a {@link DisplayList} onto a Java2D surface. It can render straight to an off-screen
 * {@link BufferedImage} (used by tests and thumbnailing) or onto any {@link Graphics2D} provided by a
 * Swing component, so the same paint code drives both the headless and on-screen paths.
 */
public final class Painter {

    private Painter() {
    }

    public static BufferedImage paint(DisplayList list, int width, int height) {
        int w = Math.max(1, width);
        int h = Math.max(1, height);
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new java.awt.Color(255, 255, 255));
        g.fillRect(0, 0, w, h);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        paintTo(g, list);
        g.dispose();
        return image;
    }

    public static void paintTo(Graphics2D g, DisplayList list) {
        for (DisplayCommand command : list.commands()) {
            if (command instanceof DisplayCommand.SolidRect r) {
                g.setColor(toAwt(r.color()));
                g.fillRect(round(r.x()), round(r.y()), round(r.width()), round(r.height()));
            } else if (command instanceof DisplayCommand.Glyphs t) {
                g.setColor(toAwt(t.color()));
                g.setFont(new Font(Font.SANS_SERIF, t.bold() ? Font.BOLD : Font.PLAIN, round(t.fontSize())));
                FontMetrics fm = g.getFontMetrics();
                g.drawString(t.text(), round(t.x()), round(t.y()) + fm.getAscent());
            }
        }
    }

    private static java.awt.Color toAwt(io.argus.browser.style.Color c) {
        return new java.awt.Color(c.r(), c.g(), c.b(), c.a());
    }

    private static int round(double v) {
        return (int) Math.round(v);
    }
}
