package io.argus.browser.layout;

import io.argus.browser.style.StyledNode;
import io.argus.browser.style.Value;
import java.util.ArrayList;
import java.util.List;

/**
 * A node in the layout tree and the block-layout algorithm that positions it. Each box is either a
 * block box (its children are other block boxes) or an anonymous block wrapping a run of inline
 * content. Inline content is laid out into {@link LineBox}es with real word wrapping.
 *
 * <p>The block width/position/height calculation follows the CSS 2.1 normal-flow rules for
 * block-level boxes in a block formatting context (auto width absorbs available space, auto margins
 * center, over-constrained boxes adjust the right margin). Floats, positioning schemes, and inline
 * block boxes are intentionally out of scope.
 */
public final class LayoutBox {

    public enum Kind {
        BLOCK,
        ANONYMOUS
    }

    private static final String[] SIDES = {"top", "right", "bottom", "left"};

    private final Kind kind;
    private final StyledNode style; // null for anonymous boxes
    private final TextMeasurer measurer;
    private final Dimensions dimensions = new Dimensions();
    private final List<LayoutBox> children = new ArrayList<>();
    private final List<TextRun> inlineRuns = new ArrayList<>();
    private final List<LineBox> lines = new ArrayList<>();

    LayoutBox(Kind kind, StyledNode style, TextMeasurer measurer) {
        this.kind = kind;
        this.style = style;
        this.measurer = measurer;
    }

    public Kind kind() {
        return kind;
    }

    public StyledNode style() {
        return style;
    }

    public Dimensions dimensions() {
        return dimensions;
    }

    public List<LayoutBox> children() {
        return children;
    }

    public List<LineBox> lines() {
        return lines;
    }

    List<TextRun> inlineRuns() {
        return inlineRuns;
    }

    // ---- Layout ---------------------------------------------------------------------------------

    /** Lays out this box (and its subtree) inside the given containing block. */
    public void layout(Dimensions containingBlock) {
        calculateBlockWidth(containingBlock);
        calculateBlockPosition(containingBlock);
        if (children.isEmpty()) {
            layoutInline();
        } else {
            layoutBlockChildren();
        }
        applyExplicitHeight();
    }

    private void calculateBlockWidth(Dimensions cb) {
        double cbWidth = cb.content.width;

        Double width = specifiedLength("width", cbWidth); // null => auto
        Double ml = marginEdge(3, cbWidth); // null => auto
        Double mr = marginEdge(1, cbWidth);
        double pl = paddingEdge(3, cbWidth);
        double pr = paddingEdge(1, cbWidth);
        double bl = borderEdge(3, cbWidth);
        double br = borderEdge(1, cbWidth);

        double total = nz(width) + nz(ml) + nz(mr) + pl + pr + bl + br;
        boolean wAuto = width == null;
        boolean mlAuto = ml == null;
        boolean mrAuto = mr == null;

        if (!wAuto && total > cbWidth) {
            if (mlAuto) {
                ml = 0.0;
                mlAuto = false;
            }
            if (mrAuto) {
                mr = 0.0;
                mrAuto = false;
            }
        }

        double underflow = cbWidth - total;
        if (!wAuto && !mlAuto && !mrAuto) {
            mr = nz(mr) + underflow;
        } else if (!wAuto && !mlAuto && mrAuto) {
            mr = underflow;
        } else if (!wAuto && mlAuto && !mrAuto) {
            ml = underflow;
        } else if (wAuto) {
            if (mlAuto) {
                ml = 0.0;
            }
            if (mrAuto) {
                mr = 0.0;
            }
            if (underflow >= 0) {
                width = underflow;
            } else {
                width = 0.0;
                mr = nz(mr) + underflow;
            }
        } else {
            ml = underflow / 2.0;
            mr = underflow / 2.0;
        }

        double pt = paddingEdge(0, cbWidth);
        double pb = paddingEdge(2, cbWidth);
        double bt = borderEdge(0, cbWidth);
        double bb = borderEdge(2, cbWidth);
        double mt = nz(marginEdge(0, cbWidth));
        double mb = nz(marginEdge(2, cbWidth));

        dimensions.content.width = nz(width);
        dimensions.padding = new EdgeSizes(pt, pr, pb, pl);
        dimensions.border = new EdgeSizes(bt, br, bb, bl);
        dimensions.margin = new EdgeSizes(mt, nz(mr), mb, nz(ml));
    }

    private void calculateBlockPosition(Dimensions cb) {
        Rect c = dimensions.content;
        c.x = cb.content.x + dimensions.margin.left() + dimensions.border.left() + dimensions.padding.left();
        c.y = cb.content.y + cb.content.height
                + dimensions.margin.top() + dimensions.border.top() + dimensions.padding.top();
    }

    private void layoutBlockChildren() {
        for (LayoutBox child : children) {
            child.layout(dimensions);
            dimensions.content.height += child.dimensions.marginBox().height;
        }
    }

    private void applyExplicitHeight() {
        if (style == null) {
            return;
        }
        if (style.value("height") instanceof Value.Length l && !l.isPercent()) {
            dimensions.content.height = l.resolve(fontSize());
        }
    }

    private void layoutInline() {
        lines.clear();
        double startX = dimensions.content.x;
        double maxWidth = dimensions.content.width;
        double cursorX = startX;
        double lineTop = dimensions.content.y;
        double lineHeight = 0;
        LineBox current = new LineBox(lineTop);

        for (TextRun run : inlineRuns) {
            double fs = run.style().length("font-size", 16);
            boolean bold = isBold(run.style());
            double lh = measurer.lineHeight(fs);
            double space = measurer.textWidth(" ", fs, bold);
            for (String word : splitWords(run.text())) {
                double w = measurer.textWidth(word, fs, bold);
                if (cursorX > startX && cursorX + w > startX + maxWidth) {
                    current.setHeight(lineHeight);
                    lines.add(current);
                    lineTop += lineHeight;
                    current = new LineBox(lineTop);
                    cursorX = startX;
                    lineHeight = 0;
                }
                current.fragments().add(new InlineFragment(word, run.style(), new Rect(cursorX, lineTop, w, lh)));
                cursorX += w + space;
                lineHeight = Math.max(lineHeight, lh);
            }
        }
        if (!current.fragments().isEmpty()) {
            current.setHeight(lineHeight);
            lines.add(current);
        }

        double total = 0;
        for (LineBox lb : lines) {
            total += lb.height();
        }
        dimensions.content.height = total;
    }

    // ---- Style value helpers --------------------------------------------------------------------

    private double fontSize() {
        return style != null ? style.length("font-size", 16) : 16;
    }

    /** Returns the property resolved to pixels, or {@code null} if it is auto/absent/not a length. */
    private Double specifiedLength(String prop, double cbWidth) {
        if (style == null) {
            return null;
        }
        if (style.value(prop) instanceof Value.Length l) {
            return resolveLength(l, cbWidth);
        }
        return null;
    }

    private double resolveLength(Value.Length l, double cbWidth) {
        return l.isPercent() ? l.amount() / 100.0 * cbWidth : l.resolve(fontSize());
    }

    /** Margin for a side (0=top..3=left); {@code null} means the margin is {@code auto}. */
    private Double marginEdge(int side, double cbWidth) {
        return edge("margin", side, cbWidth, true);
    }

    private double paddingEdge(int side, double cbWidth) {
        return nz(edge("padding", side, cbWidth, false));
    }

    private Double edge(String base, int side, double cbWidth, boolean allowAuto) {
        if (style == null) {
            return 0.0;
        }
        Value longhand = style.value(base + "-" + SIDES[side]);
        if (longhand != null) {
            return coerceEdge(longhand, cbWidth, allowAuto);
        }
        Value shorthand = style.value(base);
        if (shorthand == null) {
            return 0.0;
        }
        if (shorthand instanceof Value.Length l) {
            return resolveLength(l, cbWidth);
        }
        if (shorthand instanceof Value.Keyword k) {
            if (allowAuto && k.name().equals("auto")) {
                return null;
            }
            return parseShorthand(k.name(), cbWidth)[side];
        }
        return 0.0;
    }

    private Double coerceEdge(Value v, double cbWidth, boolean allowAuto) {
        if (v instanceof Value.Length l) {
            return resolveLength(l, cbWidth);
        }
        if (allowAuto && v instanceof Value.Keyword k && k.name().equals("auto")) {
            return null;
        }
        return 0.0;
    }

    private double[] parseShorthand(String text, double cbWidth) {
        List<Double> nums = new ArrayList<>();
        for (String token : text.trim().split("\\s+")) {
            if (token.isEmpty()) {
                continue;
            }
            nums.add(Value.parse(token) instanceof Value.Length l ? resolveLength(l, cbWidth) : 0.0);
        }
        double t;
        double r;
        double b;
        double l;
        switch (nums.size()) {
            case 1 -> {
                t = r = b = l = nums.get(0);
            }
            case 2 -> {
                t = b = nums.get(0);
                r = l = nums.get(1);
            }
            case 3 -> {
                t = nums.get(0);
                r = l = nums.get(1);
                b = nums.get(2);
            }
            case 4 -> {
                t = nums.get(0);
                r = nums.get(1);
                b = nums.get(2);
                l = nums.get(3);
            }
            default -> {
                t = r = b = l = 0;
            }
        }
        return new double[] {t, r, b, l};
    }

    private double borderEdge(int side, double cbWidth) {
        if (style == null) {
            return 0.0;
        }
        if (style.value("border-" + SIDES[side] + "-width") instanceof Value.Length l) {
            return resolveLength(l, cbWidth);
        }
        Value borderWidth = style.value("border-width");
        if (borderWidth instanceof Value.Length l) {
            return resolveLength(l, cbWidth);
        }
        if (borderWidth instanceof Value.Keyword k) {
            return parseShorthand(k.name(), cbWidth)[side];
        }
        return firstLength(style.value("border"), cbWidth);
    }

    private double firstLength(Value shorthand, double cbWidth) {
        if (shorthand instanceof Value.Length l) {
            return resolveLength(l, cbWidth);
        }
        if (shorthand instanceof Value.Keyword k) {
            for (String token : k.name().trim().split("\\s+")) {
                if (Value.parse(token) instanceof Value.Length l) {
                    return resolveLength(l, cbWidth);
                }
            }
        }
        return 0.0;
    }

    private static boolean isBold(StyledNode s) {
        Value v = s.value("font-weight");
        if (v instanceof Value.Keyword k) {
            return k.name().equals("bold") || k.name().equals("bolder");
        }
        return v instanceof Value.Length l && l.amount() >= 700;
    }

    private static List<String> splitWords(String text) {
        List<String> out = new ArrayList<>();
        for (String w : text.trim().split("\\s+")) {
            if (!w.isEmpty()) {
                out.add(w);
            }
        }
        return out;
    }

    private static double nz(Double d) {
        return d == null ? 0.0 : d;
    }
}
