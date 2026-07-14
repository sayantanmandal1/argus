package io.argus.browser.style;

import io.argus.browser.css.CssParser;
import io.argus.browser.css.Declaration;
import io.argus.browser.css.Rule;
import io.argus.browser.css.Selector;
import io.argus.browser.css.Specificity;
import io.argus.browser.css.Stylesheet;
import io.argus.browser.dom.Document;
import io.argus.browser.dom.Element;
import io.argus.browser.dom.Node;
import io.argus.browser.dom.Text;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Resolves the cascade: given a DOM tree and a set of author stylesheets, it produces a
 * {@link StyledNode} tree whose values are the winning declarations after ordering by origin,
 * specificity, and source order, with inline {@code style} attributes taking precedence and a fixed
 * set of properties inherited from parent to child.
 *
 * <p>This is a faithful but bounded cascade: it does not implement {@code !important}, at-rules
 * ({@code @media}, {@code @font-face}), or shorthand expansion beyond what the value parser handles.
 */
public final class StyleResolver {

    /** Properties that inherit by default in CSS (a practical subset). */
    private static final Set<String> INHERITED = Set.of(
            "color", "font-family", "font-size", "font-weight", "font-style",
            "line-height", "text-align", "text-indent", "text-transform",
            "white-space", "visibility", "list-style", "letter-spacing", "word-spacing");

    private final List<Stylesheet> sheets;

    /** Builds a resolver whose cascade is the user-agent sheet followed by the given author sheets. */
    public StyleResolver(List<Stylesheet> authorSheets) {
        List<Stylesheet> all = new ArrayList<>();
        all.add(UserAgentStyles.stylesheet());
        all.addAll(authorSheets);
        this.sheets = List.copyOf(all);
    }

    /**
     * Convenience entry point: collects CSS from every {@code <style>} element in the document and
     * styles the whole tree.
     */
    public static StyledNode resolve(Document document) {
        List<Stylesheet> author = new ArrayList<>();
        CssParser parser = new CssParser();
        for (Element style : document.getElementsByTagName("style")) {
            author.add(parser.parse(style.textContent()));
        }
        return new StyleResolver(author).styleTree(document);
    }

    public StyledNode styleTree(Node root) {
        return style(root, Map.of());
    }

    private StyledNode style(Node node, Map<String, Value> inherited) {
        Map<String, Value> values = new LinkedHashMap<>(inherited);
        if (node instanceof Element el) {
            values.putAll(specifiedValues(el));
        }
        Map<String, Value> forChildren = inheritedSubset(values);
        List<StyledNode> kids = new ArrayList<>();
        for (Node child : node.childNodes()) {
            if (child instanceof Element || child instanceof Text) {
                kids.add(style(child, forChildren));
            }
        }
        return new StyledNode(node, values, kids);
    }

    private Map<String, Value> inheritedSubset(Map<String, Value> values) {
        Map<String, Value> out = new LinkedHashMap<>();
        for (Map.Entry<String, Value> e : values.entrySet()) {
            if (INHERITED.contains(e.getKey())) {
                out.put(e.getKey(), e.getValue());
            }
        }
        return out;
    }

    private record MatchedRule(int origin, Specificity spec, int order, List<Declaration> decls) {
    }

    private Map<String, Value> specifiedValues(Element el) {
        List<MatchedRule> matched = new ArrayList<>();
        int order = 0;
        for (int s = 0; s < sheets.size(); s++) {
            int origin = (s == 0) ? 0 : 1; // sheet 0 is the user-agent stylesheet
            for (Rule rule : sheets.get(s).rules()) {
                Specificity best = bestMatch(rule, el);
                if (best != null) {
                    matched.add(new MatchedRule(origin, best, order, rule.declarations()));
                }
                order++;
            }
        }
        matched.sort((a, b) -> {
            if (a.origin != b.origin) {
                return Integer.compare(a.origin, b.origin);
            }
            int cmp = a.spec.compareTo(b.spec);
            return cmp != 0 ? cmp : Integer.compare(a.order, b.order);
        });

        Map<String, Value> values = new LinkedHashMap<>();
        for (MatchedRule mr : matched) {
            for (Declaration d : mr.decls()) {
                values.put(d.property().toLowerCase(Locale.ROOT), Value.parse(d.value()));
            }
        }
        applyInlineStyle(el, values);
        return values;
    }

    private Specificity bestMatch(Rule rule, Element el) {
        Specificity best = null;
        for (Selector sel : rule.selectors()) {
            if (sel.matches(el)) {
                Specificity sp = sel.specificity();
                if (best == null || sp.compareTo(best) > 0) {
                    best = sp;
                }
            }
        }
        return best;
    }

    private void applyInlineStyle(Element el, Map<String, Value> values) {
        String inline = el.getAttribute("style");
        if (inline == null || inline.isBlank()) {
            return;
        }
        Stylesheet parsed = new CssParser().parse("* { " + inline + " }");
        if (parsed.rules().isEmpty()) {
            return;
        }
        for (Declaration d : parsed.rules().get(0).declarations()) {
            values.put(d.property().toLowerCase(Locale.ROOT), Value.parse(d.value()));
        }
    }
}
