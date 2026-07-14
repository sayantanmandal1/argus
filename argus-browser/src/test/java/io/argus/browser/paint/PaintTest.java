package io.argus.browser.paint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.argus.browser.dom.Document;
import io.argus.browser.html.HtmlParser;
import io.argus.browser.layout.Layout;
import io.argus.browser.layout.LayoutBox;
import io.argus.browser.style.Color;
import io.argus.browser.style.StyleResolver;
import io.argus.browser.style.StyledNode;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

class PaintTest {

    private static LayoutBox layout(String html, double width) {
        Document doc = new HtmlParser().parse(html);
        StyledNode styled = StyleResolver.resolve(doc);
        return Layout.layoutDocument(styled, width);
    }

    @Test
    void backgroundColorFillsPixels() {
        LayoutBox root = layout(
                "<html><body><div style=\"background-color:#ff0000;height:50px\"></div></body></html>", 800);
        BufferedImage image = Painter.paint(DisplayList.build(root), 800, 200);
        // The div's content sits at (8,8) with the 8px body margin; sample well inside it.
        java.awt.Color pixel = new java.awt.Color(image.getRGB(20, 20), true);
        assertEquals(255, pixel.getRed());
        assertEquals(0, pixel.getGreen());
        assertEquals(0, pixel.getBlue());
    }

    @Test
    void areaOutsideBoxStaysWhiteBackground() {
        LayoutBox root = layout(
                "<html><body><div style=\"background-color:#ff0000;height:20px\"></div></body></html>", 800);
        BufferedImage image = Painter.paint(DisplayList.build(root), 800, 400);
        java.awt.Color pixel = new java.awt.Color(image.getRGB(400, 380), true);
        assertEquals(255, pixel.getRed());
        assertEquals(255, pixel.getGreen());
        assertEquals(255, pixel.getBlue());
    }

    @Test
    void displayListContainsBackgroundAndText() {
        LayoutBox root = layout("<html><body><p style=\"background-color:#00ff00\">Hi</p></body></html>", 800);
        DisplayList list = DisplayList.build(root);
        boolean hasGreen = list.commands().stream().anyMatch(c ->
                c instanceof DisplayCommand.SolidRect r && r.color().equals(new Color(0, 255, 0, 255)));
        boolean hasText = list.commands().stream().anyMatch(c ->
                c instanceof DisplayCommand.Glyphs t && t.text().equals("Hi"));
        assertTrue(hasGreen, "expected a green background rectangle");
        assertTrue(hasText, "expected a glyph run for the text");
    }

    @Test
    void borderEmitsFourEdgeRectangles() {
        LayoutBox root = layout(
                "<html><body><div style=\"border-width:5px;border-color:#0000ff;height:20px\"></div></body></html>",
                800);
        DisplayList list = DisplayList.build(root);
        long blueRects = list.commands().stream().filter(c ->
                c instanceof DisplayCommand.SolidRect r && r.color().equals(new Color(0, 0, 255, 255))).count();
        assertEquals(4, blueRects);
    }

    @Test
    void headingTextUsesUserAgentColorByDefault() {
        LayoutBox root = layout("<html><body><a href=\"/x\">link</a></body></html>", 800);
        DisplayList list = DisplayList.build(root);
        // The user-agent stylesheet colors links #0000ee; the glyphs should carry that color.
        boolean linkColored = list.commands().stream().anyMatch(c ->
                c instanceof DisplayCommand.Glyphs t && t.color().equals(new Color(0, 0, 0xee, 255)));
        assertTrue(linkColored, "expected link text to inherit the user-agent link color");
    }
}
