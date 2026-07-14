package io.argus.browser.style;

import java.util.Locale;
import java.util.Map;

/**
 * An RGBA color with 8-bit channels. Parses the CSS color forms an author is most likely to write:
 * named colors, {@code #rgb}/{@code #rrggbb}/{@code #rrggbbaa} hex, and {@code rgb()}/{@code rgba()}
 * functions. Unknown input yields {@code null} so the caller can fall back to a keyword value.
 */
public record Color(int r, int g, int b, int a) {

    public Color {
        r = clamp(r);
        g = clamp(g);
        b = clamp(b);
        a = clamp(a);
    }

    public static Color rgb(int r, int g, int b) {
        return new Color(r, g, b, 255);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private static final Map<String, Color> NAMED = Map.ofEntries(
            Map.entry("transparent", new Color(0, 0, 0, 0)),
            Map.entry("black", rgb(0, 0, 0)),
            Map.entry("silver", rgb(192, 192, 192)),
            Map.entry("gray", rgb(128, 128, 128)),
            Map.entry("grey", rgb(128, 128, 128)),
            Map.entry("white", rgb(255, 255, 255)),
            Map.entry("maroon", rgb(128, 0, 0)),
            Map.entry("red", rgb(255, 0, 0)),
            Map.entry("purple", rgb(128, 0, 128)),
            Map.entry("fuchsia", rgb(255, 0, 255)),
            Map.entry("magenta", rgb(255, 0, 255)),
            Map.entry("green", rgb(0, 128, 0)),
            Map.entry("lime", rgb(0, 255, 0)),
            Map.entry("olive", rgb(128, 128, 0)),
            Map.entry("yellow", rgb(255, 255, 0)),
            Map.entry("navy", rgb(0, 0, 128)),
            Map.entry("blue", rgb(0, 0, 255)),
            Map.entry("teal", rgb(0, 128, 128)),
            Map.entry("aqua", rgb(0, 255, 255)),
            Map.entry("cyan", rgb(0, 255, 255)),
            Map.entry("orange", rgb(255, 165, 0)),
            Map.entry("pink", rgb(255, 192, 203)),
            Map.entry("brown", rgb(165, 42, 42)),
            Map.entry("gold", rgb(255, 215, 0)),
            Map.entry("indigo", rgb(75, 0, 130)),
            Map.entry("violet", rgb(238, 130, 238)),
            Map.entry("lightgray", rgb(211, 211, 211)),
            Map.entry("lightgrey", rgb(211, 211, 211)),
            Map.entry("darkgray", rgb(169, 169, 169)),
            Map.entry("darkgrey", rgb(169, 169, 169)));

    /** Parses a CSS color, or returns {@code null} if the text is not a recognizable color. */
    public static Color parse(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) {
            return null;
        }
        if (s.charAt(0) == '#') {
            return parseHex(s.substring(1));
        }
        if (s.startsWith("rgb(") || s.startsWith("rgba(")) {
            return parseFunction(s);
        }
        return NAMED.get(s);
    }

    private static Color parseHex(String h) {
        try {
            return switch (h.length()) {
                case 3 -> new Color(hex(h, 0, 1) * 17, hex(h, 1, 2) * 17, hex(h, 2, 3) * 17, 255);
                case 6 -> new Color(hex(h, 0, 2), hex(h, 2, 4), hex(h, 4, 6), 255);
                case 8 -> new Color(hex(h, 0, 2), hex(h, 2, 4), hex(h, 4, 6), hex(h, 6, 8));
                default -> null;
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int hex(String s, int from, int to) {
        return Integer.parseInt(s.substring(from, to), 16);
    }

    private static Color parseFunction(String s) {
        int open = s.indexOf('(');
        int close = s.indexOf(')');
        if (open < 0 || close < open) {
            return null;
        }
        String[] parts = s.substring(open + 1, close).split(",");
        try {
            if (parts.length == 3) {
                return new Color(channel(parts[0]), channel(parts[1]), channel(parts[2]), 255);
            }
            if (parts.length == 4) {
                int alpha = (int) Math.round(Double.parseDouble(parts[3].trim()) * 255);
                return new Color(channel(parts[0]), channel(parts[1]), channel(parts[2]), alpha);
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return null;
    }

    private static int channel(String p) {
        String v = p.trim();
        if (v.endsWith("%")) {
            double pct = Double.parseDouble(v.substring(0, v.length() - 1));
            return (int) Math.round(pct * 255.0 / 100.0);
        }
        return (int) Math.round(Double.parseDouble(v));
    }
}
