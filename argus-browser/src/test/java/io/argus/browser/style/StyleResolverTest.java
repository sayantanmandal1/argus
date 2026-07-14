package io.argus.browser.style;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.argus.browser.dom.Document;
import io.argus.browser.dom.Element;
import io.argus.browser.html.HtmlParser;
import org.junit.jupiter.api.Test;

class StyleResolverTest {

    private static StyledNode styleOf(StyledNode root, String tag) {
        if (root.node() instanceof Element e && e.tagName().equals(tag)) {
            return root;
        }
        for (StyledNode child : root.children()) {
            StyledNode found = styleOf(child, tag);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static StyledNode tree(String html) {
        Document doc = new HtmlParser().parse(html);
        return StyleResolver.resolve(doc);
    }

    @Test
    void userAgentGivesBlockAndInlineDefaults() {
        StyledNode root = tree("<div><span>hi</span></div>");
        assertEquals("block", styleOf(root, "div").display());
        assertEquals("inline", styleOf(root, "span").display());
    }

    @Test
    void headAndTitleAreDisplayNone() {
        StyledNode root = tree("<html><head><title>t</title></head><body>x</body></html>");
        assertEquals("none", styleOf(root, "head").display());
        assertEquals("none", styleOf(root, "title").display());
    }

    @Test
    void authorStyleFromStyleElementApplies() {
        StyledNode root = tree("<html><head><style>p { color: red; }</style></head>"
                + "<body><p>hello</p></body></html>");
        assertEquals(new Color(255, 0, 0, 255), styleOf(root, "p").color("color"));
    }

    @Test
    void idSelectorBeatsClassSelectorRegardlessOfOrder() {
        StyledNode root = tree("<html><head><style>"
                + ".box { color: green; } #hero { color: blue; }"
                + "</style></head><body>"
                + "<div id=\"hero\" class=\"box\">x</div></body></html>");
        assertEquals(new Color(0, 0, 255, 255), styleOf(root, "div").color("color"));
    }

    @Test
    void colorInheritsFromAncestor() {
        StyledNode root = tree("<html><head><style>body { color: #112233; }</style></head>"
                + "<body><p>child</p></body></html>");
        assertEquals(new Color(0x11, 0x22, 0x33, 255), styleOf(root, "p").color("color"));
    }

    @Test
    void inlineStyleAttributeOverridesStylesheet() {
        StyledNode root = tree("<html><head><style>p { color: red; }</style></head>"
                + "<body><p style=\"color: #00ff00;\">x</p></body></html>");
        assertEquals(new Color(0, 255, 0, 255), styleOf(root, "p").color("color"));
    }

    @Test
    void parsesLengthWithUnit() {
        Value v = Value.parse("12px");
        assertInstanceOf(Value.Length.class, v);
        assertEquals(12.0, ((Value.Length) v).amount());
        assertEquals(Unit.PX, ((Value.Length) v).unit());
    }

    @Test
    void parsesShortHexColor() {
        Value v = Value.parse("#0f0");
        assertInstanceOf(Value.ColorValue.class, v);
        assertEquals(new Color(0, 255, 0, 255), ((Value.ColorValue) v).color());
    }

    @Test
    void unknownValueBecomesKeyword() {
        assertInstanceOf(Value.Keyword.class, Value.parse("sans-serif"));
    }

    @Test
    void headingGetsUserAgentFontSize() {
        StyledNode root = tree("<h1>Title</h1>");
        assertNotNull(styleOf(root, "h1"));
        assertEquals(32.0, styleOf(root, "h1").length("font-size", 0));
    }
}
