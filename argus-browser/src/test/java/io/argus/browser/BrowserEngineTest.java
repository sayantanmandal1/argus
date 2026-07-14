package io.argus.browser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.argus.browser.layout.Layout;
import io.argus.browser.layout.LayoutBox;
import io.argus.browser.net.HttpResourceLoader;
import io.argus.browser.net.ResourceLoader;
import io.argus.browser.paint.AwtTextMeasurer;
import io.argus.browser.style.Color;
import io.argus.browser.style.StyledNode;
import io.argus.browser.dom.Element;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BrowserEngineTest {

    /** A loader backed by a fixed map, so the engine can be exercised without any network. */
    private static final class FakeLoader implements ResourceLoader {
        private final Map<String, String> resources = new HashMap<>();

        FakeLoader put(String uri, String content) {
            resources.put(uri, content);
            return this;
        }

        @Override
        public String fetchText(URI uri) throws IOException {
            String content = resources.get(uri.toString());
            if (content == null) {
                throw new IOException("404 " + uri);
            }
            return content;
        }
    }

    private static StyledNode find(StyledNode node, String tag) {
        if (node.node() instanceof Element e && e.tagName().equals(tag)) {
            return node;
        }
        for (StyledNode child : node.children()) {
            StyledNode found = find(child, tag);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    @Test
    void inlineStyleElementIsApplied() {
        BrowserEngine engine = new BrowserEngine(new FakeLoader());
        Page page = engine.parse(
                "<html><head><style>p{color:#ff0000}</style></head><body><p>hi</p></body></html>",
                URI.create("http://example.com/"));
        assertEquals(new Color(255, 0, 0, 255), find(page.styledRoot(), "p").color("color"));
    }

    @Test
    void externalStylesheetIsFetchedAndApplied() {
        FakeLoader loader = new FakeLoader().put("http://example.com/site.css", "p { color: #0000ff; }");
        BrowserEngine engine = new BrowserEngine(loader);
        Page page = engine.parse(
                "<html><head><link rel=\"stylesheet\" href=\"site.css\"></head><body><p>hi</p></body></html>",
                URI.create("http://example.com/index.html"));
        assertEquals(new Color(0, 0, 255, 255), find(page.styledRoot(), "p").color("color"));
    }

    @Test
    void baseHrefResolvesRelativeStylesheetUrl() {
        FakeLoader loader = new FakeLoader().put("http://cdn.example.com/a.css", "p { color: #00ff00; }");
        BrowserEngine engine = new BrowserEngine(loader);
        Page page = engine.parse(
                "<html><head><base href=\"http://cdn.example.com/\">"
                        + "<link rel=\"stylesheet\" href=\"a.css\"></head><body><p>hi</p></body></html>",
                URI.create("http://example.com/index.html"));
        assertEquals(new Color(0, 255, 0, 255), find(page.styledRoot(), "p").color("color"));
    }

    @Test
    void missingStylesheetIsToleratedNotFatal() {
        BrowserEngine engine = new BrowserEngine(new FakeLoader());
        Page page = engine.parse(
                "<html><head><link rel=\"stylesheet\" href=\"gone.css\"></head><body><p>hi</p></body></html>",
                URI.create("http://example.com/"));
        // The page still parses and styles; the unreachable sheet is simply dropped.
        assertEquals("hi", find(page.styledRoot(), "p").node().textContent());
    }

    @Test
    void titleIsExtracted() {
        BrowserEngine engine = new BrowserEngine(new FakeLoader());
        Page page = engine.parse(
                "<html><head><title>  Hello Argus  </title></head><body></body></html>",
                URI.create("http://example.com/"));
        assertEquals("Hello Argus", page.title());
    }

    @Test
    void rendersPageToImageThatFitsContent() {
        BrowserEngine engine = new BrowserEngine(new FakeLoader());
        Page page = engine.parse(
                "<html><body><div style=\"height:500px\"></div></body></html>",
                URI.create("http://example.com/"));
        BufferedImage image = engine.render(page, 800, 100);
        assertEquals(800, image.getWidth());
        assertTrue(image.getHeight() >= 500, "image should grow to fit tall content");
    }

    @Test
    void offlineLayoutUsesRealFontMetrics() {
        // Sanity check that the AWT-backed measurer produces a positive width (headless-safe).
        assertTrue(new AwtTextMeasurer().textWidth("Argus", 16, false) > 0);
    }

    @Test
    void httpLoaderRejectsNonHttpSchemes() {
        HttpResourceLoader loader = new HttpResourceLoader();
        assertThrows(IOException.class, () -> loader.fetchText(URI.create("file:///etc/passwd")));
    }

    @Test
    void emptyDocumentStillLaysOut() {
        BrowserEngine engine = new BrowserEngine(new FakeLoader());
        Page page = engine.parse("", URI.create("http://example.com/"));
        LayoutBox root = Layout.layoutDocument(page.styledRoot(), 800, new AwtTextMeasurer());
        assertEquals(800, root.dimensions().content.width, 0.001);
    }

    @Test
    void inlineScriptMutationAppearsInStyledTree() {
        BrowserEngine engine = new BrowserEngine(new FakeLoader()); // scripting enabled by default
        Page page = engine.parse(
                "<html><body><p id=\"x\">old</p>"
                        + "<script>document.getElementById('x').textContent = 'NEW';</script>"
                        + "</body></html>",
                URI.create("http://example.com/"));
        assertEquals("NEW", find(page.styledRoot(), "p").node().textContent());
    }

    @Test
    void scriptingCanBeDisabled() {
        BrowserEngine engine = new BrowserEngine(new FakeLoader(), false);
        Page page = engine.parse(
                "<html><body><p id=\"x\">old</p>"
                        + "<script>document.getElementById('x').textContent = 'NEW';</script>"
                        + "</body></html>",
                URI.create("http://example.com/"));
        assertEquals("old", find(page.styledRoot(), "p").node().textContent());
    }
}
