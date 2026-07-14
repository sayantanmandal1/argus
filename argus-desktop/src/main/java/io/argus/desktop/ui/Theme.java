package io.argus.desktop.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

/**
 * The visual language for the desktop app: a cohesive dark "browser chrome" palette, a font stack
 * that prefers the platform UI font, and a handful of safe global Swing defaults. Everything else is
 * custom-painted, so the look is identical regardless of the active Look and Feel.
 */
public final class Theme {

    private Theme() {
    }

    // Surfaces (darkest -> lightest).
    public static final Color WINDOW = new Color(0x0d, 0x0e, 0x12);
    public static final Color STRIP = new Color(0x10, 0x12, 0x17);   // tab strip
    public static final Color CHROME = new Color(0x18, 0x1b, 0x22);  // toolbar / active tab
    public static final Color PANEL = new Color(0x15, 0x18, 0x1e);   // cards, popups
    public static final Color RAISED = new Color(0x23, 0x27, 0x30);  // hover / selection
    public static final Color FIELD = new Color(0x0b, 0x0c, 0x10);   // omnibox interior

    // Lines and text.
    public static final Color LINE = new Color(0x28, 0x2d, 0x36);
    public static final Color LINE_SOFT = new Color(0x20, 0x24, 0x2c);
    public static final Color TEXT = new Color(0xec, 0xed, 0xf1);
    public static final Color DIM = new Color(0x97, 0x9e, 0xab);
    public static final Color FAINT = new Color(0x66, 0x6d, 0x7a);

    // Accents.
    public static final Color EMBER = new Color(0xff, 0x6a, 0x2b);
    public static final Color EMBER_DEEP = new Color(0xe0, 0x53, 0x18);
    public static final Color BLUE = new Color(0x4c, 0x8d, 0xff);
    public static final Color GREEN = new Color(0x35, 0xd0, 0x9b);

    public static final Font UI = uiFont(Font.PLAIN, 13f);
    public static final Font UI_MEDIUM = uiFont(Font.PLAIN, 13f);
    public static final Font UI_BOLD = uiFont(Font.BOLD, 13f);
    public static final Font MONO = monoFont(Font.PLAIN, 13f);

    private static final Set<String> FAMILIES =
            Set.of(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());

    /** Returns the UI font at a given style/size, preferring the platform's native UI typeface. */
    public static Font uiFont(int style, float size) {
        String family = firstAvailable("Segoe UI Variable Text", "Segoe UI", "Inter", "SF Pro Text",
                "Helvetica Neue", "Roboto", Font.SANS_SERIF);
        return new Font(family, style, Math.round(size)).deriveFont(size);
    }

    /** Returns a monospaced font at a given style/size. */
    public static Font monoFont(int style, float size) {
        String family = firstAvailable("Cascadia Code", "JetBrains Mono", "Consolas", "SF Mono",
                "Menlo", Font.MONOSPACED);
        return new Font(family, style, Math.round(size)).deriveFont(size);
    }

    private static String firstAvailable(String... names) {
        for (String name : names) {
            if (Font.SANS_SERIF.equals(name) || Font.MONOSPACED.equals(name) || FAMILIES.contains(name)) {
                return name;
            }
        }
        return Font.SANS_SERIF;
    }

    /** A translucent overlay used for hover/pressed states over any surface. */
    public static Color overlay(int alpha) {
        return new Color(0xff, 0xff, 0xff, Math.max(0, Math.min(255, alpha)));
    }

    /** Lightens a colour towards white by the given fraction (0..1). */
    public static Color lighten(Color c, double f) {
        return new Color(
                (int) Math.round(c.getRed() + (255 - c.getRed()) * f),
                (int) Math.round(c.getGreen() + (255 - c.getGreen()) * f),
                (int) Math.round(c.getBlue() + (255 - c.getBlue()) * f));
    }

    /** Enables antialiasing (shapes and text) on a graphics context. */
    public static void aa(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    /** Builds the application/taskbar icon: a rounded ember tile with a bright "A" glyph. */
    public static BufferedImage appIcon(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        aa(g);
        int r = Math.round(size * 0.24f);
        g.setColor(EMBER);
        g.fillRoundRect(0, 0, size, size, r, r);
        g.setColor(EMBER_DEEP);
        g.fillRoundRect(0, Math.round(size * 0.62f), size, Math.round(size * 0.38f), r, r);
        g.setColor(new Color(0x12, 0x10, 0x0e));
        g.setFont(uiFont(Font.BOLD, size * 0.62f));
        var fm = g.getFontMetrics();
        String a = "A";
        int tx = (size - fm.stringWidth(a)) / 2;
        int ty = (size - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(a, tx, ty);
        g.dispose();
        return img;
    }

    /**
     * Installs a small, Look-and-Feel-agnostic set of defaults for the few standard widgets that are
     * not custom-painted (popup menus, tooltips). Backgrounds of dialogs are intentionally left
     * alone so their default dark-on-light text stays readable.
     */
    public static void install() {
        UIManager.put("ToolTip.background", RAISED);
        UIManager.put("ToolTip.foreground", TEXT);
        UIManager.put("ToolTip.font", UI);
        UIManager.put("ToolTip.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE), new EmptyBorder(4, 8, 4, 8)));

        UIManager.put("PopupMenu.background", PANEL);
        UIManager.put("PopupMenu.border", BorderFactory.createLineBorder(LINE));
        UIManager.put("MenuItem.background", PANEL);
        UIManager.put("MenuItem.foreground", TEXT);
        UIManager.put("MenuItem.selectionBackground", RAISED);
        UIManager.put("MenuItem.selectionForeground", TEXT);
        UIManager.put("MenuItem.font", UI);
        UIManager.put("MenuItem.acceleratorForeground", DIM);
        UIManager.put("MenuItem.acceleratorSelectionForeground", TEXT);
        UIManager.put("Separator.foreground", LINE);
        UIManager.put("Separator.background", PANEL);
    }
}
