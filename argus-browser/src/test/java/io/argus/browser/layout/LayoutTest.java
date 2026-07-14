package io.argus.browser.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.argus.browser.dom.Document;
import io.argus.browser.dom.Element;
import io.argus.browser.html.HtmlParser;
import io.argus.browser.style.StyleResolver;
import io.argus.browser.style.StyledNode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class LayoutTest {

    private static LayoutBox layout(String html, double width) {
        Document doc = new HtmlParser().parse(html);
        StyledNode styled = StyleResolver.resolve(doc);
        return Layout.layoutDocument(styled, width);
    }

    private static LayoutBox box(LayoutBox root, String tag) {
        if (root.style() != null && root.style().node() instanceof Element e && e.tagName().equals(tag)) {
            return root;
        }
        for (LayoutBox child : root.children()) {
            LayoutBox found = box(child, tag);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static List<LayoutBox> allByTag(LayoutBox root, String tag, List<LayoutBox> acc) {
        if (root.style() != null && root.style().node() instanceof Element e && e.tagName().equals(tag)) {
            acc.add(root);
        }
        for (LayoutBox child : root.children()) {
            allByTag(child, tag, acc);
        }
        return acc;
    }

    @Test
    void blockFillsContainingWidthMinusMargins() {
        LayoutBox root = layout("<html><body><div></div></body></html>", 800);
        // body has an 8px user-agent margin on each side, so its content is 784 wide;
        // the div (auto width, no margin) fills that.
        assertEquals(784.0, box(root, "div").dimensions().content.width, 0.001);
    }

    @Test
    void blocksStackVertically() {
        LayoutBox root = layout(
                "<html><body><div style=\"height:50px\"></div><div style=\"height:30px\"></div></body></html>",
                800);
        List<LayoutBox> divs = allByTag(root, "div", new ArrayList<>());
        assertEquals(2, divs.size());
        double y1 = divs.get(0).dimensions().content.y;
        double y2 = divs.get(1).dimensions().content.y;
        assertEquals(8.0, y1, 0.001); // starts at body's content top (8px margin)
        assertEquals(50.0, y2 - y1, 0.001); // second sits directly below the 50px-tall first
    }

    @Test
    void parentHeightWrapsChildren() {
        LayoutBox root = layout(
                "<html><body><div style=\"height:40px\"></div><div style=\"height:20px\"></div></body></html>",
                800);
        assertEquals(60.0, box(root, "body").dimensions().content.height, 0.001);
    }

    @Test
    void explicitHeightIsApplied() {
        LayoutBox root = layout("<html><body><div style=\"height:123px\"></div></body></html>", 800);
        assertEquals(123.0, box(root, "div").dimensions().content.height, 0.001);
    }

    @Test
    void longTextWrapsIntoMultipleLines() {
        String words = "word ".repeat(100);
        LayoutBox root = layout("<html><body><p>" + words + "</p></body></html>", 200);
        LayoutBox p = box(root, "p");
        assertTrue(p.lines().size() > 1, "expected the paragraph to wrap onto multiple lines");
        assertTrue(p.dimensions().content.height > 0);
    }

    @Test
    void displayNoneProducesNoBox() {
        LayoutBox root = layout(
                "<html><body><div></div><span style=\"display:none\">hidden</span></body></html>", 800);
        assertNull(box(root, "span"));
    }

    @Test
    void inlineFragmentsCarryWordsInOrder() {
        LayoutBox root = layout("<html><body><p>Hello world</p></body></html>", 800);
        LayoutBox p = box(root, "p");
        List<String> texts = new ArrayList<>();
        for (LineBox line : p.lines()) {
            line.fragments().forEach(f -> texts.add(f.text()));
        }
        assertEquals(List.of("Hello", "world"), texts);
    }

    @Test
    void anonymousBoxWrapsInlineRunsBesideBlocks() {
        LayoutBox root = layout("<html><body>loose text<div></div></body></html>", 800);
        LayoutBox body = box(root, "body");
        // body mixes an inline run ("loose text") with a block <div>, so the inline run must be
        // wrapped in an anonymous block box that precedes the div.
        assertEquals(LayoutBox.Kind.ANONYMOUS, body.children().get(0).kind());
        assertTrue(body.children().get(0).lines().size() >= 1);
    }
}
