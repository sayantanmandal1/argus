import io.argus.browser.BrowserEngine;
import io.argus.browser.Page;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import javax.imageio.ImageIO;

/**
 * Renders a rich local page and (if the network allows) a real website to PNGs, to demonstrate the
 * from-scratch engine end to end. Run with the module classes on the classpath:
 *   java -Djava.awt.headless=true -cp argus-browser/target/classes demo/RenderDemo.java
 */
public class RenderDemo {

    private static final String SAMPLE = """
        <!doctype html>
        <html>
          <head>
            <title>Argus Engine Demo</title>
            <style>
              body { background-color: #f4f6fb; font-size: 16px; color: #202124; }
              h1 { color: #1a3c8c; font-size: 34px; }
              h2 { color: #2b2b2b; }
              .card { background-color: #ffffff; border-width: 1px; border-color: #d0d7de;
                      padding: 16px; margin: 16px; }
              .accent { background-color: #1a73e8; color: #ffffff; padding: 12px; margin: 16px; }
              .tag { color: #0a7d33; font-weight: bold; }
              a { color: #1155cc; }
              p { color: #3c4043; }
            </style>
          </head>
          <body>
            <h1>Argus Browser Engine</h1>
            <div class="accent">A from-scratch HTML + CSS layout and paint engine, written in Java.</div>
            <div class="card">
              <h2>What works</h2>
              <p>The engine tokenizes HTML into a DOM, parses CSS, runs the cascade with
                 specificity and inheritance, lays out the block and inline boxes with real word
                 wrapping, and paints the result with Java2D.</p>
              <p class="tag">Static pages render. JavaScript does not run yet.</p>
              <p>Learn more at <a href="https://example.com">the project page</a>.</p>
            </div>
            <div class="card">
              <h2>Box model</h2>
              <div style="background-color:#ffe08a;height:40px;margin:8px"></div>
              <div style="background-color:#a0e0a0;height:40px;margin:8px"></div>
              <div style="background-color:#f4a0a0;height:40px;margin:8px"></div>
            </div>
          </body>
        </html>
        """;

    private static final String JS_DEMO = """
        <!doctype html>
        <html>
          <head>
            <title>Scripted</title>
            <style>
              body { font-size: 16px; color: #202124; background-color: #ffffff; }
              h1 { color: #6b21a8; }
              .item { background-color: #ede9fe; border-width: 1px; border-color: #c4b5fd;
                      padding: 10px; margin: 8px; color: #4c1d95; }
              #total { color: #065f46; font-weight: bold; margin: 8px; }
            </style>
          </head>
          <body>
            <h1>Built by JavaScript</h1>
            <div id="list"></div>
            <p id="total"></p>
            <script>
              var fruits = ['Apples', 'Bananas', 'Cherries', 'Dates'];
              var list = document.getElementById('list');
              for (var i = 0; i < fruits.length; i++) {
                var row = document.createElement('div');
                row.className = 'item';
                row.textContent = (i + 1) + '. ' + fruits[i];
                list.appendChild(row);
              }
              document.getElementById('total').textContent =
                'Total items: ' + fruits.length + ' (rendered from a for-loop in the JS engine)';
            </script>
          </body>
        </html>
        """;

    public static void main(String[] args) throws Exception {
        File outDir = new File("demo");
        outDir.mkdirs();

        BrowserEngine engine = new BrowserEngine();
        Page page = engine.parse(SAMPLE, URI.create("https://argus.local/"));
        BufferedImage local = engine.render(page, 900, 700);
        ImageIO.write(local, "png", new File(outDir, "sample.png"));
        System.out.println("Rendered local sample: " + local.getWidth() + "x" + local.getHeight()
                + "  title=\"" + page.title() + "\"");

        Page scripted = engine.parse(JS_DEMO, URI.create("https://argus.local/js"));
        BufferedImage scriptedImage = engine.render(scripted, 900, 600);
        ImageIO.write(scriptedImage, "png", new File(outDir, "scripted.png"));
        System.out.println("Rendered JS-built page: " + scriptedImage.getWidth() + "x" + scriptedImage.getHeight());

        for (String url : new String[] {"http://neverssl.com", "http://info.cern.ch/hypertext/WWW/TheProject.html"}) {
            try {
                BufferedImage real = engine.render(url, 1000, 800);
                String name = url.replaceAll("[^a-zA-Z0-9]", "_") + ".png";
                ImageIO.write(real, "png", new File(outDir, name));
                System.out.println("Rendered REAL site " + url + " -> " + name
                        + " (" + real.getWidth() + "x" + real.getHeight() + ")");
            } catch (Exception e) {
                System.out.println("Could not render " + url + ": " + e.getClass().getSimpleName()
                        + " - " + e.getMessage());
            }
        }
    }
}
