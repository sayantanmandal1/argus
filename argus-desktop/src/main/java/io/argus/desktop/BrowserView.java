package io.argus.desktop;

import io.argus.browser.BrowserEngine;
import io.argus.browser.Page;
import io.argus.browser.layout.Layout;
import io.argus.browser.layout.LayoutBox;
import io.argus.browser.paint.AwtTextMeasurer;
import io.argus.browser.paint.DisplayList;
import io.argus.browser.paint.Painter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

/**
 * A minimal web view backed by the in-process Argus browser engine. It fetches a URL (or renders a
 * bundled welcome page), lays the document out to the viewport width, and paints the resulting
 * display list with Java2D. Fetching runs off the event thread; layout and paint run on it.
 *
 * <p>Honest scope: this renders static HTML/CSS and runs the engine's JavaScript subset. It is not a
 * production browser — JS-heavy single-page apps will show only their initial markup.
 */
public final class BrowserView extends JPanel {

    private static final String WELCOME =
            """
            <!doctype html>
            <html><head><title>Argus Web</title>
            <style>
              body { background-color: #ffffff; color: #202124; font-size: 16px; margin: 24px; }
              h1 { color: #1a3c8c; }
              .note { background-color: #eef2ff; border-width: 1px; border-color: #c7d2fe;
                      padding: 14px; margin: 14px; color: #313a5a; }
              .warn { color: #8a5300; }
              code { color: #0a7d33; }
            </style></head>
            <body>
              <h1>Argus Web Renderer</h1>
              <div class="note">This page is drawn by a from-scratch HTML + CSS + JavaScript engine
                 written in Java &mdash; no browser component is embedded.</div>
              <p>Type a URL above and press <code>Go</code>. Simple static sites render well; try
                 <code>http://info.cern.ch/hypertext/WWW/TheProject.html</code>.</p>
              <p class="warn">JavaScript-heavy apps will show only their initial server markup.</p>
            </body></html>
            """;

    private final BrowserEngine engine = new BrowserEngine();
    private final JTextField urlField = new JTextField();
    private final RenderCanvas canvas = new RenderCanvas();
    private final JLabel status = new JLabel(" Ready");
    private final JScrollPane scroll = new JScrollPane();
    private final JButton backButton = new JButton("\u25C0");
    private final JButton forwardButton = new JButton("\u25B6");

    private final List<String> history = new ArrayList<>();
    private int historyIndex = -1;
    private Page currentPage;

    public BrowserView() {
        super(new BorderLayout());
        add(buildBar(), BorderLayout.NORTH);

        scroll.setViewportView(canvas);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);

        status.setBorder(new EmptyBorder(5, 8, 5, 8));
        add(status, BorderLayout.SOUTH);

        scroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                relayout(); // reflow to the new width
            }
        });

        SwingUtilities.invokeLater(this::showWelcome);
    }

    private JPanel buildBar() {
        JPanel bar = new JPanel(new BorderLayout(6, 0));
        bar.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        backButton.setEnabled(false);
        forwardButton.setEnabled(false);
        backButton.addActionListener(e -> goBack());
        forwardButton.addActionListener(e -> goForward());
        JButton reload = new JButton("\u21BB");
        reload.addActionListener(e -> {
            if (currentPage != null) {
                load(currentPage.url().toString(), false);
            }
        });
        nav.add(backButton);
        nav.add(forwardButton);
        nav.add(reload);
        bar.add(nav, BorderLayout.WEST);

        urlField.addActionListener(e -> load(urlField.getText(), true));
        bar.add(urlField, BorderLayout.CENTER);

        JButton go = new JButton("Go");
        go.addActionListener(e -> load(urlField.getText(), true));
        bar.add(go, BorderLayout.EAST);
        return bar;
    }

    /** Loads a URL, optionally pushing it onto the back/forward history. */
    public void load(String rawUrl, boolean pushHistory) {
        String url = normalize(rawUrl);
        if (url.isEmpty()) {
            return;
        }
        urlField.setText(url);
        status.setText(" Loading " + url + " \u2026");
        new SwingWorker<Page, Void>() {
            @Override
            protected Page doInBackground() throws Exception {
                return engine.load(url);
            }

            @Override
            protected void done() {
                try {
                    currentPage = get();
                    if (pushHistory) {
                        pushHistory(url);
                    }
                    relayout();
                    status.setText(" " + (currentPage.title().isEmpty() ? url : currentPage.title()));
                } catch (Exception ex) {
                    status.setText(" Could not load " + url + ": " + rootMessage(ex));
                }
            }
        }.execute();
    }

    private void showWelcome() {
        currentPage = engine.parse(WELCOME, URI.create("about:welcome"));
        relayout();
        status.setText(" Argus Web Renderer");
    }

    private void relayout() {
        if (currentPage == null) {
            return;
        }
        int width = Math.max(400, scroll.getViewport().getWidth() - 4);
        LayoutBox root = Layout.layoutDocument(currentPage.styledRoot(), width, new AwtTextMeasurer());
        canvas.setLayoutRoot(root, width);
    }

    private void goBack() {
        if (historyIndex > 0) {
            historyIndex--;
            load(history.get(historyIndex), false);
            updateNavButtons();
        }
    }

    private void goForward() {
        if (historyIndex < history.size() - 1) {
            historyIndex++;
            load(history.get(historyIndex), false);
            updateNavButtons();
        }
    }

    private void pushHistory(String url) {
        while (history.size() > historyIndex + 1) {
            history.remove(history.size() - 1);
        }
        history.add(url);
        historyIndex = history.size() - 1;
        updateNavButtons();
    }

    private void updateNavButtons() {
        backButton.setEnabled(historyIndex > 0);
        forwardButton.setEnabled(historyIndex < history.size() - 1);
    }

    private static String normalize(String raw) {
        String url = raw == null ? "" : raw.trim();
        if (url.isEmpty() || url.contains("://")) {
            return url;
        }
        return "http://" + url;
    }

    private static String rootMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return message == null ? cause.getClass().getSimpleName() : message;
    }

    /** The scrollable surface that paints the laid-out page. */
    private static final class RenderCanvas extends JComponent {

        private LayoutBox root;

        void setLayoutRoot(LayoutBox root, int width) {
            this.root = root;
            int height = root == null ? 0 : (int) Math.ceil(root.dimensions().marginBox().height);
            setPreferredSize(new Dimension(width, Math.max(height, 120)));
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (root != null) {
                Painter.paintTo(g2, DisplayList.build(root));
            }
            g2.dispose();
        }
    }
}
