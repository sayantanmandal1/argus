package io.argus.browser.style;

import io.argus.browser.css.CssParser;
import io.argus.browser.css.Stylesheet;

/**
 * The built-in user-agent stylesheet. Real browsers ship a large default sheet; this is a compact
 * version that establishes the block/inline distinction and a few familiar defaults (heading sizes,
 * link color, default margins) so a page without any author CSS still lays out sensibly.
 */
public final class UserAgentStyles {

    private UserAgentStyles() {
    }

    public static final String CSS =
            """
            html, body, div, p, h1, h2, h3, h4, h5, h6, ul, ol, li, section, article,
            header, footer, nav, main, aside, figure, figcaption, blockquote, pre, table,
            thead, tbody, tr, td, th, form, fieldset, address, dl, dd, dt, hr {
                display: block;
            }
            head, script, style, title, meta, link, base, noscript { display: none; }
            span, a, b, i, em, strong, small, code, label, img, button, sub, sup {
                display: inline;
            }
            body { margin: 8px; color: #000000; font-size: 16px; font-family: sans-serif; }
            h1 { font-size: 32px; font-weight: bold; margin: 16px; }
            h2 { font-size: 24px; font-weight: bold; margin: 14px; }
            h3 { font-size: 20px; font-weight: bold; margin: 12px; }
            h4 { font-size: 16px; font-weight: bold; margin: 12px; }
            h5 { font-size: 14px; font-weight: bold; margin: 10px; }
            h6 { font-size: 12px; font-weight: bold; margin: 10px; }
            p { margin: 12px; }
            ul, ol { margin: 12px; padding: 24px; }
            blockquote { margin: 12px; padding: 8px; }
            pre, code { font-family: monospace; }
            a { color: #0000ee; }
            strong, b { font-weight: bold; }
            em, i { font-style: italic; }
            """;

    private static final Stylesheet SHEET = new CssParser().parse(CSS);

    public static Stylesheet stylesheet() {
        return SHEET;
    }
}
