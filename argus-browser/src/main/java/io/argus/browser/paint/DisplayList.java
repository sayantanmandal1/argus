package io.argus.browser.paint;

import io.argus.browser.layout.Dimensions;
import io.argus.browser.layout.InlineFragment;
import io.argus.browser.layout.LayoutBox;
import io.argus.browser.layout.LineBox;
import io.argus.browser.layout.Rect;
import io.argus.browser.style.Color;
import io.argus.browser.style.StyledNode;
import io.argus.browser.style.Value;
import java.util.ArrayList;
import java.util.List;

/**
 * Flattens a laid-out box tree into an ordered {@link DisplayCommand} list. For each box the order is
 * background, then borders, then this box's inline text, then children — the natural back-to-front
 * paint order for normal flow without z-index.
 */
public final class DisplayList {

    private final List<DisplayCommand> commands = new ArrayList<>();

    public List<DisplayCommand> commands() {
        return commands;
    }

    public static DisplayList build(LayoutBox root) {
        DisplayList list = new DisplayList();
        list.renderBox(root);
        return list;
    }

    private void renderBox(LayoutBox box) {
        paintBackground(box);
        paintBorders(box);
        for (LineBox line : box.lines()) {
            for (InlineFragment fragment : line.fragments()) {
                paintText(fragment);
            }
        }
        for (LayoutBox child : box.children()) {
            renderBox(child);
        }
    }

    private void paintBackground(LayoutBox box) {
        StyledNode style = box.style();
        if (style == null) {
            return;
        }
        Color bg = style.color("background-color");
        if (bg == null) {
            bg = style.color("background");
        }
        if (bg == null || bg.a() == 0) {
            return;
        }
        Rect r = box.dimensions().borderBox();
        commands.add(new DisplayCommand.SolidRect(r.x, r.y, r.width, r.height, bg));
    }

    private void paintBorders(LayoutBox box) {
        StyledNode style = box.style();
        if (style == null) {
            return;
        }
        Color color = style.color("border-color");
        if (color == null) {
            color = borderShorthandColor(style);
        }
        if (color == null) {
            color = style.color("color"); // CSS currentColor fallback
        }
        if (color == null) {
            return;
        }
        Dimensions d = box.dimensions();
        Rect border = d.borderBox();
        Rect padding = d.paddingBox();
        if (d.border.left() > 0) {
            commands.add(new DisplayCommand.SolidRect(border.x, border.y, d.border.left(), border.height, color));
        }
        if (d.border.right() > 0) {
            commands.add(new DisplayCommand.SolidRect(
                    padding.x + padding.width, border.y, d.border.right(), border.height, color));
        }
        if (d.border.top() > 0) {
            commands.add(new DisplayCommand.SolidRect(border.x, border.y, border.width, d.border.top(), color));
        }
        if (d.border.bottom() > 0) {
            commands.add(new DisplayCommand.SolidRect(
                    border.x, padding.y + padding.height, border.width, d.border.bottom(), color));
        }
    }

    private void paintText(InlineFragment fragment) {
        StyledNode style = fragment.style();
        Color color = style.color("color");
        if (color == null) {
            color = Color.rgb(0, 0, 0);
        }
        double fontSize = style.length("font-size", 16);
        boolean bold = isBold(style);
        Rect box = fragment.box();
        commands.add(new DisplayCommand.Glyphs(fragment.text(), box.x, box.y, fontSize, bold, color));
    }

    private static Color borderShorthandColor(StyledNode style) {
        Value v = style.value("border");
        if (v instanceof Value.ColorValue cv) {
            return cv.color();
        }
        if (v instanceof Value.Keyword k) {
            for (String token : k.name().trim().split("\\s+")) {
                Color c = Color.parse(token);
                if (c != null) {
                    return c;
                }
            }
        }
        return null;
    }

    private static boolean isBold(StyledNode style) {
        Value v = style.value("font-weight");
        if (v instanceof Value.Keyword k) {
            return k.name().equals("bold") || k.name().equals("bolder");
        }
        return v instanceof Value.Length l && l.amount() >= 700;
    }
}
