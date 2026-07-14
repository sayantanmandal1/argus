package io.argus.browser.layout;

import io.argus.browser.dom.Element;
import io.argus.browser.dom.Text;
import io.argus.browser.style.StyledNode;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds a {@link LayoutBox} tree from a styled DOM tree and runs the layout pass against a viewport.
 *
 * <p>The builder applies the CSS rule that a block box may contain either block-level boxes or inline
 * content, never both: when a block element mixes them, runs of inline content are wrapped in
 * anonymous block boxes. Elements with {@code display: none} are dropped entirely.
 */
public final class Layout {

    private Layout() {
    }

    public static LayoutBox layoutDocument(StyledNode styledRoot, double viewportWidth) {
        return layoutDocument(styledRoot, viewportWidth, TextMeasurer.DEFAULT);
    }

    public static LayoutBox layoutDocument(StyledNode styledRoot, double viewportWidth, TextMeasurer measurer) {
        LayoutBox root = buildRoot(styledRoot, measurer);
        root.layout(viewport(viewportWidth));
        return root;
    }

    private static Dimensions viewport(double width) {
        Dimensions d = new Dimensions();
        d.content = new Rect(0, 0, width, 0);
        return d;
    }

    /**
     * Builds the root layout box. If the styled root is itself an element, that element is the root;
     * otherwise (a {@code #document} node, or a fragment) an anonymous block establishes the viewport
     * and lays out every top-level flow child. This handles legacy pages that omit {@code <html>} and
     * leave {@code <head>}/{@code <body>}-level content as document siblings.
     */
    static LayoutBox buildRoot(StyledNode node, TextMeasurer measurer) {
        if (node.node() instanceof Element) {
            return buildBox(node, measurer);
        }
        LayoutBox root = new LayoutBox(LayoutBox.Kind.BLOCK, null, measurer);
        buildChildren(root, node.children(), measurer);
        return root;
    }

    static LayoutBox buildBox(StyledNode element, TextMeasurer measurer) {
        LayoutBox box = new LayoutBox(LayoutBox.Kind.BLOCK, element, measurer);
        buildChildren(box, element.children(), measurer);
        return box;
    }

    private static void buildChildren(LayoutBox box, List<StyledNode> childNodes, TextMeasurer measurer) {
        boolean anyBlockChild = false;
        for (StyledNode child : childNodes) {
            if (!isDisplayNone(child) && isBlockLevel(child)) {
                anyBlockChild = true;
                break;
            }
        }

        if (anyBlockChild) {
            List<TextRun> pending = new ArrayList<>();
            for (StyledNode child : childNodes) {
                if (isDisplayNone(child)) {
                    continue;
                }
                if (isBlockLevel(child)) {
                    flushAnonymous(box, pending, measurer);
                    box.children().add(buildBox(child, measurer));
                } else {
                    collectRuns(child, pending);
                }
            }
            flushAnonymous(box, pending, measurer);
        } else {
            for (StyledNode child : childNodes) {
                collectRuns(child, box.inlineRuns());
            }
        }
    }

    private static void flushAnonymous(LayoutBox parent, List<TextRun> pending, TextMeasurer measurer) {
        if (pending.isEmpty()) {
            return;
        }
        LayoutBox anonymous = new LayoutBox(LayoutBox.Kind.ANONYMOUS, null, measurer);
        anonymous.inlineRuns().addAll(pending);
        parent.children().add(anonymous);
        pending.clear();
    }

    private static void collectRuns(StyledNode node, List<TextRun> out) {
        if (node.node() instanceof Text text) {
            if (!text.data().isBlank()) {
                out.add(new TextRun(text.data(), node));
            }
            return;
        }
        if (isDisplayNone(node)) {
            return;
        }
        for (StyledNode child : node.children()) {
            collectRuns(child, out);
        }
    }

    private static boolean isBlockLevel(StyledNode node) {
        if (!(node.node() instanceof Element)) {
            return false;
        }
        String display = node.display();
        return display.equals("block")
                || display.equals("list-item")
                || display.equals("table")
                || display.equals("flex")
                || display.equals("grid");
    }

    private static boolean isDisplayNone(StyledNode node) {
        return node.node() instanceof Element && node.display().equals("none");
    }
}
