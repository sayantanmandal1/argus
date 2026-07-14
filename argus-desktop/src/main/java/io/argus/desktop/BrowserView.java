package io.argus.desktop;

import io.argus.browser.BrowserEngine;
import io.argus.browser.Page;
import io.argus.browser.layout.Layout;
import io.argus.browser.layout.LayoutBox;
import io.argus.browser.paint.AwtTextMeasurer;
import io.argus.browser.paint.DisplayList;
import io.argus.browser.paint.Painter;
import io.argus.desktop.ui.FlatButton;
import io.argus.desktop.ui.FlatScrollBarUI;
import io.argus.desktop.ui.Icons;
import io.argus.desktop.ui.LoadingBar;
import io.argus.desktop.ui.Omnibox;
import io.argus.desktop.ui.Theme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

/**
 * A single browser tab backed by the in-process Argus engine, dressed to look like a modern browser:
 * a rounded omnibox with a security indicator, circular toolbar buttons, a bookmarks strip, a
 * sliding load indicator, and keyboard shortcuts. Fetching runs off the event thread; layout and
 * paint run on it.
 *
 * <p>Honest scope: this renders static HTML/CSS and runs the engine's JavaScript subset. It is not a
 * production browser — JS-heavy single-page apps will show only their initial markup.
 */
public final class BrowserView extends JPanel {

    private static final String WELCOME =
            """
            <!doctype html>
            <html><head><title>New Tab</title>
            <style>
              body { background-color: #0f1116; color: #ecedf1; font-size: 16px; margin: 0; padding: 0; }
              .wrap { margin: 56px; }
              .badge { color: #ff6a2b; font-size: 13px; margin: 0 0 10px; }
              h1 { color: #ffffff; font-size: 34px; margin: 0 0 10px; }
              .lede { color: #979eab; font-size: 17px; margin: 0 0 28px; }
              .card { background-color: #171a21; border-width: 1px; border-color: #282d36;
                      padding: 18px; margin: 0 0 14px; color: #c9cdd6; }
              .card h3 { color: #ecedf1; font-size: 16px; margin: 0 0 6px; }
              b { color: #ff6a2b; }
              code { color: #35d09b; }
            </style></head>
            <body>
              <div class="wrap">
                <p class="badge">ARGUS BROWSER ENGINE</p>
                <h1>New Tab</h1>
                <p class="lede">A from-scratch HTML, CSS and JavaScript engine written in Java &mdash;
                   no web component is embedded. Type a URL in the bar above and press <b>Enter</b>.</p>
                <div class="card">
                  <h3>Try a real website</h3>
                  Open <code>http://info.cern.ch/hypertext/WWW/TheProject.html</code> &mdash; the first
                  website ever published &mdash; and watch it render.
                </div>
                <div class="card">
                  <h3>Handwritten pipeline</h3>
                  HTML is tokenized into a DOM, JavaScript mutates it, CSS cascades into computed
                  styles, boxes are laid out, and Java2D paints the pixels.
                </div>
                <div class="card">
                  <h3>Shortcuts</h3>
                  <code>Ctrl+L</code> address bar &middot; <code>Ctrl+R</code> reload &middot;
                  <code>Ctrl+T</code> new tab &middot; <code>Alt+Left/Right</code> history.
                </div>
              </div>
            </body></html>
            """;

    private static final String HOME_URL = "about:welcome";

    private final BrowserEngine engine = new BrowserEngine();
    private final Omnibox omnibox = new Omnibox("Search or type a URL");
    private final JTextField urlField = omnibox.field();
    private final RenderCanvas canvas = new RenderCanvas();
    private final JScrollPane scroll = new JScrollPane();
    private final JLabel status = new JLabel("Ready");
    private final LoadingBar loadingBar = new LoadingBar();

    private final FlatButton backButton = FlatButton.icon(Icons.Kind.BACK, "Back", null);
    private final FlatButton forwardButton = FlatButton.icon(Icons.Kind.FORWARD, "Forward", null);
    private final FlatButton reloadButton = FlatButton.icon(Icons.Kind.RELOAD, "Reload", null);

    private final List<String> history = new ArrayList<>();
    private int historyIndex = -1;
    private Page currentPage;
    private SwingWorker<Page, Void> worker;
    private boolean loading;

    private final Runnable onNewTab;
    private Consumer<String> titleListener;

    public BrowserView(Runnable onNewTab) {
        super(new BorderLayout());
        this.onNewTab = onNewTab;
        setBackground(Theme.CHROME);

        add(buildTop(), BorderLayout.NORTH);

        canvas.setFocusable(true);
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                canvas.requestFocusInWindow();
            }
        });
        scroll.setViewportView(canvas);
        scroll.getVerticalScrollBar().setUnitIncrement(28);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(new Color(0xf1, 0xf3, 0xf4));
        FlatScrollBarUI.apply(scroll);
        add(scroll, BorderLayout.CENTER);

        status.setForeground(Theme.DIM);
        status.setFont(Theme.UI);
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(Theme.PANEL);
        statusBar.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, Theme.LINE_SOFT), new EmptyBorder(5, 14, 5, 14)));
        statusBar.add(status, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        backButton.addActionListener(e -> goBack());
        forwardButton.addActionListener(e -> goForward());
        reloadButton.addActionListener(e -> {
            if (loading) {
                stopLoading();
            } else if (currentPage != null) {
                load(currentPage.url().toString(), false);
            }
        });

        installShortcuts();

        scroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                relayout();
            }
        });

        SwingUtilities.invokeLater(this::showWelcome);
    }

    private JComponent buildTop() {
        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setBackground(Theme.CHROME);
        toolbar.setBorder(new EmptyBorder(8, 10, 8, 12));

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        nav.setOpaque(false);
        FlatButton home = FlatButton.icon(Icons.Kind.HOME, "Home", e -> showWelcome());
        backButton.setEnabled(false);
        forwardButton.setEnabled(false);
        nav.add(backButton);
        nav.add(forwardButton);
        nav.add(reloadButton);
        nav.add(home);
        toolbar.add(nav, BorderLayout.WEST);

        urlField.addActionListener(e -> load(urlField.getText(), true));
        toolbar.add(omnibox, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        actions.setOpaque(false);
        if (onNewTab != null) {
            actions.add(FlatButton.icon(Icons.Kind.PLUS, "New tab (Ctrl+T)", e -> onNewTab.run()));
        }
        actions.add(FlatButton.icon(Icons.Kind.STAR, "Quick links are below", e ->
                setStatus("Use the quick links below the address bar.")));
        toolbar.add(actions, BorderLayout.EAST);

        JPanel bookmarks = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        bookmarks.setBackground(Theme.CHROME);
        bookmarks.setBorder(new MatteBorder(1, 0, 1, 0, Theme.LINE_SOFT));
        addChip(bookmarks, "Start", HOME_URL);
        addChip(bookmarks, "CERN — first website", "http://info.cern.ch/hypertext/WWW/TheProject.html");
        addChip(bookmarks, "example.com", "http://example.com/");
        addChip(bookmarks, "IANA", "http://www.iana.org/domains/reserved");

        loadingBar.setAlignmentX(0f);
        loadingBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 3));
        loadingBar.setPreferredSize(new Dimension(10, 3));
        toolbar.setAlignmentX(0f);
        toolbar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        bookmarks.setAlignmentX(0f);
        bookmarks.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBackground(Theme.CHROME);
        top.add(toolbar);
        top.add(loadingBar);
        top.add(bookmarks);
        return top;
    }

    private void addChip(JPanel bar, String label, String url) {
        bar.add(FlatButton.chip(label, e -> {
            if (HOME_URL.equals(url)) {
                showWelcome();
            } else {
                load(url, true);
            }
        }));
    }

    private void installShortcuts() {
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK), "focusOmnibox", this::focusOmnibox);
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK), "reload", this::reloadCurrent);
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "reloadF5", this::reloadCurrent);
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.ALT_DOWN_MASK), "back", this::goBack);
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.ALT_DOWN_MASK), "forward", this::goForward);
    }

    private void bind(KeyStroke key, String name, Runnable action) {
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(key, name);
        getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                action.run();
            }
        });
    }

    private void reloadCurrent() {
        if (currentPage != null) {
            load(currentPage.url().toString(), false);
        }
    }

    /** Focuses the address bar and selects its contents, ready to type. */
    public void focusOmnibox() {
        urlField.requestFocusInWindow();
        urlField.selectAll();
    }

    /** Registers a callback invoked with the page title whenever this tab navigates. */
    public void setTitleListener(Consumer<String> listener) {
        this.titleListener = listener;
    }

    /** Loads a URL, optionally pushing it onto the back/forward history. */
    public void load(String rawUrl, boolean pushHistory) {
        String url = normalize(rawUrl);
        if (url.isEmpty()) {
            return;
        }
        if (HOME_URL.equals(url)) {
            showWelcome();
            return;
        }
        urlField.setText(url);
        setLoading(true);
        setStatus("Loading " + url + " \u2026");
        if (worker != null && !worker.isDone()) {
            worker.cancel(true);
        }
        worker = new SwingWorker<>() {
            @Override
            protected Page doInBackground() throws Exception {
                return engine.load(url);
            }

            @Override
            protected void done() {
                if (isCancelled()) {
                    setLoading(false);
                    return;
                }
                try {
                    currentPage = get();
                    if (pushHistory) {
                        pushHistory(url);
                    }
                    applyPageChrome();
                    relayout();
                    String title = currentPage.title().isEmpty() ? url : currentPage.title();
                    setStatus(title);
                    fireTitle(title);
                } catch (Exception ex) {
                    setStatus("Could not load " + url + ": " + rootMessage(ex));
                    omnibox.setSecurity(Omnibox.Security.INSECURE);
                    fireTitle("Untitled");
                } finally {
                    setLoading(false);
                }
            }
        };
        worker.execute();
    }

    private void stopLoading() {
        if (worker != null && !worker.isDone()) {
            worker.cancel(true);
        }
        setLoading(false);
        setStatus("Stopped");
    }

    private void showWelcome() {
        currentPage = engine.parse(WELCOME, URI.create(HOME_URL));
        urlField.setText("");
        omnibox.setSecurity(Omnibox.Security.SEARCH);
        relayout();
        setStatus("New Tab");
        fireTitle("New Tab");
        updateNavButtons();
    }

    private void applyPageChrome() {
        String scheme = currentPage.url().getScheme();
        if ("https".equalsIgnoreCase(scheme)) {
            omnibox.setSecurity(Omnibox.Security.SECURE);
        } else if ("http".equalsIgnoreCase(scheme)) {
            omnibox.setSecurity(Omnibox.Security.INSECURE);
        } else {
            omnibox.setSecurity(Omnibox.Security.SEARCH);
        }
        urlField.setText(currentPage.url().toString());
    }

    private void setLoading(boolean value) {
        this.loading = value;
        if (value) {
            reloadButton.setIcon(Icons.of(Icons.Kind.STOP, 18, Theme.TEXT));
            reloadButton.setToolTipText("Stop");
            loadingBar.start();
        } else {
            reloadButton.setIcon(Icons.of(Icons.Kind.RELOAD, 18, Theme.TEXT));
            reloadButton.setToolTipText("Reload");
            loadingBar.stop();
        }
    }

    private void fireTitle(String title) {
        if (titleListener != null) {
            titleListener.accept(title);
        }
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

    private void setStatus(String text) {
        status.setText(text);
    }

    private static String normalize(String raw) {
        String url = raw == null ? "" : raw.trim();
        if (url.isEmpty() || url.contains("://") || url.startsWith("about:")) {
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

        RenderCanvas() {
            setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        }

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
