package io.argus.browser.style;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A parsed CSS property value. The three shapes the layout and paint stages care about are keywords
 * ({@code block}, {@code bold}), lengths ({@code 12px}, {@code 1.5em}), and colors. Anything the
 * parser does not recognize as a length or color becomes a keyword.
 */
public sealed interface Value permits Value.Keyword, Value.Length, Value.ColorValue {

    Pattern LENGTH = Pattern.compile("^([+-]?\\d*\\.?\\d+)(px|em|rem|ex|pt|%)?$");

    record Keyword(String name) implements Value {
    }

    record Length(double amount, Unit unit) implements Value {
        /** Resolves to CSS pixels, using {@code emBasePx} for font-relative units. */
        public double resolve(double emBasePx) {
            return switch (unit) {
                case PX, NONE -> amount;
                case PT -> amount * (96.0 / 72.0);
                case EM, REM, EX -> amount * emBasePx;
                case PERCENT -> amount; // resolved against a container by the layout stage
            };
        }

        public boolean isPercent() {
            return unit == Unit.PERCENT;
        }
    }

    record ColorValue(Color color) implements Value {
    }

    /** Parses a single declaration value into the most specific shape that fits. */
    static Value parse(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) {
            return new Keyword("");
        }
        Matcher m = LENGTH.matcher(s);
        if (m.matches()) {
            return new Length(Double.parseDouble(m.group(1)), Unit.fromToken(m.group(2)));
        }
        Color color = Color.parse(s);
        if (color != null) {
            return new ColorValue(color);
        }
        return new Keyword(s.toLowerCase(Locale.ROOT));
    }
}
