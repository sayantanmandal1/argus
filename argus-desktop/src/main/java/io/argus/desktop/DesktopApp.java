package io.argus.desktop;

import io.argus.desktop.ui.ChromeTabbedPaneUI;
import io.argus.desktop.ui.FlatButton;
import io.argus.desktop.ui.FlatScrollBarUI;
import io.argus.desktop.ui.Icons;
import io.argus.desktop.ui.Omnibox;
import io.argus.desktop.ui.TabHeader;
import io.argus.desktop.ui.Theme;
import io.argus.index.PersistentIndex;
import io.argus.server.ArgusServer;
import io.argus.server.SearchService;
import io.argus.store.FSDirectory;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

/**
 * The Argus desktop application: a Swing UI with a browser-style navigation bar (back / forward /
 * home + a search box), ranked result cards, an "index document" dialog, index stats, and a menu
 * item that starts the embedded HTTP server so phones and other machines on the network can connect.
 *
 * <p>Search runs in-process against a durable {@link SearchService} over a local data directory.
 * Only the JDK's built-in Swing toolkit is used, so the app has no third-party dependencies.
 */
public final class DesktopApp {

    private final PersistentIndex index;
    private final SearchController controller;
    private ArgusServer server;

    private JFrame frame;
    private JTabbedPane tabs;
    private JPanel plusPage;
    private boolean addingTab;

    private JTextField searchField;
    private FlatButton backButton;
    private FlatButton forwardButton;
    private JPanel resultsPanel;
    private JLabel statusLabel;

    public DesktopApp() {
        this.index = PersistentIndex.open(new FSDirectory(Paths.get("argus-data")));
        SearchService service = new SearchService(index, "body");
        this.controller = new SearchController(service);
        seedIfEmpty(service);
    }

    private void seedIfEmpty(SearchService service) {
        if (index.numDocs() > 0) {
            return;
        }
        String[][] docs = {
            {"d1", "Distributed Systems", "fault tolerant replicated storage and consensus"},
            {"d2", "Information Retrieval", "inverted index bm25 ranking and query parsing"},
            {"d3", "Operating Systems", "virtual memory scheduling and file systems"},
            {"d4", "Optimization", "linear programming simplex and gradient descent methods"},
            {"d5", "Concurrency", "threads locks and lock free data structures"},
        };
        for (String[] d : docs) {
            service.index(Map.of("id", d[0], "title", d[1], "body", d[2]));
        }
        service.commit();
    }

    public void show() {
        Theme.install();
        frame = new JFrame("Argus");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(760, 540));
        frame.setSize(1120, 760);
        frame.setLocationRelativeTo(null);
        frame.setIconImages(List.of(
                Theme.appIcon(16), Theme.appIcon(32), Theme.appIcon(64), Theme.appIcon(128)));

        tabs = new JTabbedPane();
        ChromeTabbedPaneUI.install(tabs);
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        addingTab = true;
        tabs.addTab(null, buildSearchTab());
        TabHeader searchHeader = new TabHeader("Search", Icons.of(Icons.Kind.SEARCH, 14, Theme.EMBER), null);
        tabs.setTabComponentAt(0, searchHeader);
        attachTabSelect(searchHeader);

        plusPage = new JPanel();
        plusPage.setBackground(Theme.WINDOW);
        tabs.addTab(null, plusPage);
        tabs.setTabComponentAt(1, newTabButton());
        addingTab = false;

        tabs.addChangeListener(e -> onTabChanged());

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.WINDOW);
        root.add(tabs, BorderLayout.CENTER);
        frame.setContentPane(root);

        installAppShortcuts();
        updateTabSelection();

        frame.setVisible(true);
        runSearch(""); // browse all on start
        searchField.requestFocusInWindow();
    }

    private JComponent buildSearchTab() {
        JPanel tab = new JPanel(new BorderLayout());
        tab.setBackground(Theme.WINDOW);
        tab.add(buildSearchBar(), BorderLayout.NORTH);

        resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        resultsPanel.setBackground(Theme.WINDOW);
        resultsPanel.setBorder(new EmptyBorder(16, 18, 16, 18));

        JScrollPane scroll = new JScrollPane(resultsPanel);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        scroll.getViewport().setBackground(Theme.WINDOW);
        FlatScrollBarUI.apply(scroll);
        tab.add(scroll, BorderLayout.CENTER);

        statusLabel = new JLabel("Ready");
        statusLabel.setForeground(Theme.DIM);
        statusLabel.setFont(Theme.UI);
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(Theme.PANEL);
        statusBar.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, Theme.LINE_SOFT), new EmptyBorder(6, 16, 6, 16)));
        statusBar.add(statusLabel, BorderLayout.CENTER);
        tab.add(statusBar, BorderLayout.SOUTH);
        return tab;
    }

    private JComponent buildSearchBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setBackground(Theme.CHROME);
        bar.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, Theme.LINE_SOFT), new EmptyBorder(10, 12, 10, 12)));

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        nav.setOpaque(false);
        backButton = FlatButton.icon(Icons.Kind.BACK, "Previous search", e -> navigate(controller.goBack()));
        forwardButton = FlatButton.icon(Icons.Kind.FORWARD, "Next search", e -> navigate(controller.goForward()));
        FlatButton home = FlatButton.icon(Icons.Kind.HOME, "Browse all", e -> {
            searchField.setText("");
            runSearch("");
        });
        nav.add(backButton);
        nav.add(forwardButton);
        nav.add(home);
        bar.add(nav, BorderLayout.WEST);

        Omnibox box = new Omnibox("Search the index");
        searchField = box.field();
        searchField.addActionListener(e -> runSearch(searchField.getText()));
        bar.add(box, BorderLayout.CENTER);

        JPanel east = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        east.setOpaque(false);
        east.add(FlatButton.primary("Search", e -> runSearch(searchField.getText())));
        FlatButton menu = FlatButton.icon(Icons.Kind.MENU, "Menu", null);
        menu.addActionListener(e -> showAppMenu(menu));
        east.add(menu);
        bar.add(east, BorderLayout.EAST);

        updateNavButtons();
        return bar;
    }

    private JComponent newTabButton() {
        JLabel plus = new JLabel(Icons.of(Icons.Kind.PLUS, 16, Theme.DIM));
        plus.setBorder(new EmptyBorder(2, 10, 2, 10));
        plus.setToolTipText("New tab (Ctrl+T)");
        plus.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        plus.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                newWebTab();
            }
        });
        return plus;
    }

    private void attachTabSelect(Component header) {
        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int idx = tabs.indexOfTabComponent(header);
                if (idx >= 0) {
                    tabs.setSelectedIndex(idx);
                }
            }
        });
    }

    private void onTabChanged() {
        if (addingTab) {
            return;
        }
        int sel = tabs.getSelectedIndex();
        if (sel >= 0 && tabs.getComponentAt(sel) == plusPage) {
            newWebTab();
            return;
        }
        updateTabSelection();
    }

    private void updateTabSelection() {
        int sel = tabs.getSelectedIndex();
        for (int i = 0; i < tabs.getTabCount(); i++) {
            Component comp = tabs.getTabComponentAt(i);
            if (comp instanceof TabHeader header) {
                header.setSelected(i == sel);
            }
        }
    }

    private void newWebTab() {
        openWebTab(null);
    }

    private void openWebTab(String url) {
        BrowserView view = new BrowserView(this::newWebTab);
        int insertAt = tabs.getTabCount() - 1; // before the trailing "+" tab
        addingTab = true;
        tabs.insertTab(null, null, view, null, insertAt);
        TabHeader header = new TabHeader("New Tab", Icons.of(Icons.Kind.GLOBE, 14, Theme.DIM),
                () -> closeTab(view));
        tabs.setTabComponentAt(insertAt, header);
        attachTabSelect(header);
        view.setTitleListener(header::setTitle);
        tabs.setSelectedIndex(insertAt);
        addingTab = false;
        updateTabSelection();
        if (url != null) {
            view.load(url, true);
        }
        SwingUtilities.invokeLater(view::focusOmnibox);
    }

    private void closeTab(Component view) {
        int i = tabs.indexOfComponent(view);
        if (i < 0) {
            return;
        }
        addingTab = true;
        tabs.remove(i);
        int plusIndex = tabs.getTabCount() - 1;
        int target = Math.min(i, plusIndex - 1);
        tabs.setSelectedIndex(Math.max(0, target));
        addingTab = false;
        updateTabSelection();
    }

    private void showAppMenu(Component anchor) {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(Theme.PANEL);
        menu.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        menu.add(menuItem("New browser tab", e -> newWebTab()));
        menu.addSeparator();
        menu.add(menuItem("Index document\u2026", e -> showIndexDialog()));
        menu.add(menuItem("Commit", e -> {
            controller.commit();
            setStatus("Committed.");
        }));
        menu.add(menuItem("Index stats", e -> showStats()));
        menu.addSeparator();
        String serverLabel = server == null
                ? "Start local HTTP server"
                : "Stop server (port " + server.port() + ")";
        menu.add(menuItem(serverLabel, e -> toggleServer()));
        menu.show(anchor, 0, anchor.getHeight() + 4);
    }

    private JMenuItem menuItem(String text, ActionListener action) {
        JMenuItem item = new JMenuItem(text);
        item.setBackground(Theme.PANEL);
        item.setForeground(Theme.TEXT);
        item.setOpaque(true);
        item.setFont(Theme.UI);
        item.setBorder(new EmptyBorder(7, 14, 7, 22));
        item.addActionListener(action);
        return item;
    }

    private void installAppShortcuts() {
        JComponent root = (JComponent) frame.getContentPane();
        bindApp(root, KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK), "newTab", this::newWebTab);
        bindApp(root, KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK), "closeTab",
                this::closeCurrentTab);
    }

    private void bindApp(JComponent root, KeyStroke key, String name, Runnable action) {
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, name);
        root.getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                action.run();
            }
        });
    }

    private void closeCurrentTab() {
        int i = tabs.getSelectedIndex();
        if (i < 0) {
            return;
        }
        Component c = tabs.getComponentAt(i);
        if (c instanceof BrowserView) {
            closeTab(c);
        }
    }

    private void toggleServer() {
        try {
            if (server == null) {
                server = new ArgusServer(8080, new SearchService(index, "body"));
                server.start();
                setStatus("Serving on http://localhost:" + server.port() + "   (UI at /)");
            } else {
                server.stop();
                server = null;
                setStatus("Server stopped.");
            }
        } catch (IOException ex) {
            setStatus("Server error: " + ex.getMessage());
        }
    }

    private void showIndexDialog() {
        JTextField id = new JTextField();
        JTextField title = new JTextField();
        JTextArea body = new JTextArea(4, 24);
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        form.add(new JLabel("id"));
        form.add(id);
        form.add(new JLabel("title"));
        form.add(title);
        form.add(new JLabel("body"));
        form.add(new JScrollPane(body));
        int ok = JOptionPane.showConfirmDialog(frame, form, "Index Document", JOptionPane.OK_CANCEL_OPTION);
        if (ok == JOptionPane.OK_OPTION) {
            controller.index(Map.of("id", id.getText(), "title", title.getText(), "body", body.getText()));
            controller.commit();
            setStatus("Indexed & committed.");
            runSearch(searchField.getText());
        }
    }

    private void showStats() {
        Map<String, Object> s = controller.stats();
        JOptionPane.showMessageDialog(frame,
                "Documents: " + s.get("numDocs") + "\nMax doc: " + s.get("maxDoc")
                        + "\nGeneration: " + s.get("generation"),
                "Index Stats", JOptionPane.INFORMATION_MESSAGE);
    }

    private void runSearch(String query) {
        setStatus("Searching\u2026");
        new SwingWorker<Map<String, Object>, Void>() {
            @Override
            protected Map<String, Object> doInBackground() {
                return controller.search(query);
            }

            @Override
            protected void done() {
                try {
                    render(get());
                } catch (Exception ex) {
                    setStatus("Error: " + ex.getMessage());
                }
                updateNavButtons();
            }
        }.execute();
    }

    private void navigate(Map<String, Object> result) {
        if (result == null) {
            return;
        }
        searchField.setText(controller.currentQuery() == null ? "" : controller.currentQuery());
        render(result);
        updateNavButtons();
    }

    @SuppressWarnings("unchecked")
    private void render(Map<String, Object> result) {
        resultsPanel.removeAll();
        long total = ((Number) result.getOrDefault("total", 0L)).longValue();
        List<Object> hits = (List<Object>) result.getOrDefault("hits", List.of());
        for (Object h : hits) {
            resultsPanel.add(card((Map<String, Object>) h));
            resultsPanel.add(Box.createVerticalStrut(8));
        }
        resultsPanel.revalidate();
        resultsPanel.repaint();
        setStatus(total + " hit(s)   \u00b7   showing " + hits.size());
    }

    @SuppressWarnings("unchecked")
    private JComponent card(Map<String, Object> hit) {
        RoundedCard card = new RoundedCard();

        Map<String, Object> fields = (Map<String, Object>) hit.getOrDefault("fields", Map.of());
        String titleText = String.valueOf(fields.getOrDefault("title", fields.getOrDefault("id", "(document)")));
        JLabel title = new JLabel(titleText);
        title.setForeground(Theme.EMBER);
        title.setFont(Theme.uiFont(java.awt.Font.BOLD, 15f));

        double score = ((Number) hit.getOrDefault("score", 0.0)).doubleValue();
        JLabel meta = new JLabel(String.format("#%s   score %.4f", String.valueOf(hit.get("docId")), score));
        meta.setForeground(Theme.DIM);
        meta.setFont(Theme.MONO);

        String bodyText = String.valueOf(fields.getOrDefault("body", ""));
        JLabel body = new JLabel("<html><body style='width:720px'>" + escape(bodyText) + "</body></html>");
        body.setForeground(Theme.TEXT);
        body.setFont(Theme.UI);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(title, BorderLayout.WEST);
        top.add(meta, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);

        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
        return card;
    }

    /** A rounded result card that brightens and outlines in the accent colour on hover. */
    private static final class RoundedCard extends JPanel {

        private boolean hover;

        RoundedCard() {
            super(new BorderLayout(0, 6));
            setOpaque(false);
            setBorder(new EmptyBorder(14, 16, 14, 16));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.aa(g2);
            g2.setColor(hover ? Theme.RAISED : Theme.PANEL);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.setColor(hover ? Theme.EMBER : Theme.LINE);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void updateNavButtons() {
        if (backButton != null) {
            backButton.setEnabled(controller.canGoBack());
        }
        if (forwardButton != null) {
            forwardButton.setEnabled(controller.canGoForward());
        }
    }

    private void setStatus(String text) {
        if (statusLabel != null) {
            statusLabel.setText(" " + text);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DesktopApp().show());
    }
}
