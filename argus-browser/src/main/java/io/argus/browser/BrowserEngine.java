package io.argus.browser;

import io.argus.browser.css.CssParser;
import io.argus.browser.css.Stylesheet;
import io.argus.browser.dom.Document;
import io.argus.browser.dom.Element;
import io.argus.browser.html.HtmlParser;
import io.argus.browser.js.JsEngine;
import io.argus.browser.layout.Layout;
import io.argus.browser.layout.LayoutBox;
import io.argus.browser.net.HttpResourceLoader;
import io.argus.browser.net.ResourceLoader;
import io.argus.browser.paint.AwtTextMeasurer;
import io.argus.browser.paint.DisplayList;
import io.argus.browser.paint.Painter;
import io.argus.browser.style.StyleResolver;
import io.argus.browser.style.StyledNode;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The top-level browser engine: it ties together fetching, parsing, styling, layout, and paint. Give
 * it a URL and it returns a {@link Page} or a rendered image.
 *
 * <p>Author stylesheets are gathered in document order — inline {@code <style>} blocks and external
 * {@code <link rel="stylesheet">} sheets — so the cascade sees them in the order the page declares
 * them. A {@code <base href>} element, if present, is honored when resolving relative stylesheet
 * URLs. A stylesheet that fails to load is skipped, exactly as a real browser tolerates a missing
 * resource.
 *
 * <p>Honest scope: this renders static HTML and CSS. It does not execute JavaScript, so pages that
 * build their content at runtime with a framework will render only their initial server markup.
 */
public final class BrowserEngine {

    private final ResourceLoader loader;
    private final boolean scripting;

    public BrowserEngine() {
        this(new HttpResourceLoader(), true);
    }

    public BrowserEngine(ResourceLoader loader) {
        this(loader, true);
    }

    public BrowserEngine(ResourceLoader loader, boolean scripting) {
        this.loader = loader;
        this.scripting = scripting;
    }

    /** Fetches and parses the page at {@code url} into a styled {@link Page}. */
    public Page load(String url) throws IOException {
        URI base = URI.create(url);
        String html = loader.fetchText(base);
        return parse(html, base);
    }

    /** Parses already-fetched HTML with the given base URL (used for the offline/testing path). */
    public Page parse(String html, URI base) {
        Document document = new HtmlParser().parse(html);
        URI effectiveBase = resolveBase(document, base);
        if (scripting) {
            runScripts(document, effectiveBase);
        }
        List<Stylesheet> authorSheets = gatherAuthorSheets(document, effectiveBase);
        StyledNode styled = new StyleResolver(authorSheets).styleTree(document);
        return new Page(base, document, styled, title(document));
    }

    /** Fetches {@code url} and renders it to an image {@code width} px wide. */
    public BufferedImage render(String url, int width, int height) throws IOException {
        return render(load(url), width, height);
    }

    /** Renders an already-loaded page. The image grows to fit the content taller than {@code height}. */
    public BufferedImage render(Page page, int width, int height) {
        LayoutBox root = Layout.layoutDocument(page.styledRoot(), width, new AwtTextMeasurer());
        int contentHeight = (int) Math.ceil(root.dimensions().marginBox().height);
        return Painter.paint(DisplayList.build(root), width, Math.max(height, contentHeight));
    }

    private void runScripts(Document document, URI base) {
        JsEngine engine = new JsEngine(document);
        for (Element script : document.getElementsByTagName("script")) {
            String type = script.getAttribute("type");
            if (type != null && !type.isBlank()
                    && !type.equalsIgnoreCase("text/javascript")
                    && !type.equalsIgnoreCase("application/javascript")
                    && !type.equalsIgnoreCase("module")) {
                continue; // skip non-JS <script> blocks (JSON-LD, templates, etc.)
            }
            String code;
            String src = script.getAttribute("src");
            if (src != null && !src.isBlank()) {
                try {
                    code = loader.fetchText(base.resolve(src.trim()));
                } catch (Exception e) {
                    continue; // unreachable external script is skipped
                }
            } else {
                code = script.textContent();
            }
            if (code == null || code.isBlank()) {
                continue;
            }
            try {
                engine.run(code);
            } catch (RuntimeException e) {
                // A script error must not blank the page; skip it, as a browser logs and continues.
            }
        }
    }

    private List<Stylesheet> gatherAuthorSheets(Document document, URI base) {
        List<Stylesheet> sheets = new ArrayList<>();
        CssParser parser = new CssParser();
        for (Element el : document.getElementsByTagName("*")) {
            String tag = el.tagName();
            if (tag.equals("style")) {
                sheets.add(parser.parse(el.textContent()));
            } else if (tag.equals("link") && isStylesheetLink(el)) {
                fetchStylesheet(parser, base, el.getAttribute("href")).ifPresent(sheets::add);
            }
        }
        return sheets;
    }

    private java.util.Optional<Stylesheet> fetchStylesheet(CssParser parser, URI base, String href) {
        if (href == null || href.isBlank()) {
            return java.util.Optional.empty();
        }
        try {
            URI resolved = base.resolve(href.trim());
            return java.util.Optional.of(parser.parse(loader.fetchText(resolved)));
        } catch (Exception e) {
            return java.util.Optional.empty(); // a failed stylesheet is dropped, like a real browser
        }
    }

    private static boolean isStylesheetLink(Element link) {
        String rel = link.getAttribute("rel");
        return rel != null && rel.toLowerCase(Locale.ROOT).contains("stylesheet");
    }

    private static URI resolveBase(Document document, URI base) {
        for (Element baseEl : document.getElementsByTagName("base")) {
            String href = baseEl.getAttribute("href");
            if (href != null && !href.isBlank()) {
                try {
                    return base.resolve(href.trim());
                } catch (RuntimeException ignored) {
                    return base;
                }
            }
        }
        return base;
    }

    private static String title(Document document) {
        List<Element> titles = document.getElementsByTagName("title");
        return titles.isEmpty() ? "" : titles.get(0).textContent().trim();
    }
}
