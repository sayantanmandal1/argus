package io.argus.desktop;

import io.argus.index.PersistentIndex;
import io.argus.server.ArgusServer;
import io.argus.server.SearchService;
import io.argus.store.FSDirectory;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

/**
 * The Argus desktop application: a Swing UI with a browser-style navigation bar (back / forward /
 * home + a search box), ranked result cards, an "index document" dialog, index stats, and a menu
 * item that starts the embedded HTTP server so phones and other machines on the network can connect.
 *
 * <p>Search runs in-process against a durable {@link SearchService} over a local data directory.
 * Only the JDK's built-in Swing toolkit is used, so the app has no third-party dependencies.
 */
public final class DesktopApp {

    private static final Color BG = new Color(0x0e, 0x0f, 0x12);
    private static final Color PANEL = new Color(0x16, 0x18, 0x1d);
    private static final Color LINE = new Color(0x26, 0x2a, 0x31);
    private static final Color BONE = new Color(0xe8, 0xe6, 0xe1);
    private static final Color DIM = new Color(0x9a, 0xa0, 0xaa);
    private static final Color EMBER = new Color(0xff, 0x6a, 0x2b);

    private final PersistentIndex index;
    private final SearchController controller;
    private ArgusServer server;

    private JFrame frame;
    private JTextField searchField;
    private JButton backButton;
    private JButton forwardButton;
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
        frame = new JFrame("Argus");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(920, 680);
        frame.setLocationRelativeTo(null);
        frame.setJMenuBar(buildMenuBar());

        JPanel searchTab = new JPanel(new BorderLayout());
        searchTab.setBackground(BG);
        searchTab.add(buildNavBar(), BorderLayout.NORTH);

        resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        resultsPanel.setBackground(BG);
        resultsPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
        JScrollPane scroll = new JScrollPane(resultsPanel);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(BG);
        searchTab.add(scroll, BorderLayout.CENTER);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Search", searchTab);
        tabs.addTab("Web", new BrowserView());

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        root.add(tabs, BorderLayout.CENTER);

        statusLabel = new JLabel(" Ready");
        statusLabel.setForeground(DIM);
        statusLabel.setBackground(PANEL);
        statusLabel.setOpaque(true);
        statusLabel.setBorder(new EmptyBorder(6, 10, 6, 10));
        root.add(statusLabel, BorderLayout.SOUTH);

        frame.setContentPane(root);
        frame.setVisible(true);
        runSearch(""); // browse all on start
        searchField.requestFocusInWindow();
    }

    private JPanel buildNavBar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setBackground(PANEL);
        bar.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        nav.setOpaque(false);
        backButton = navButton("\u25C0", e -> navigate(controller.goBack()));
        forwardButton = navButton("\u25B6", e -> navigate(controller.goForward()));
        JButton home = navButton("\u2302", e -> {
            searchField.setText("");
            runSearch("");
        });
        nav.add(backButton);
        nav.add(forwardButton);
        nav.add(home);
        bar.add(nav, BorderLayout.WEST);

        searchField = new JTextField();
        searchField.setBackground(BG);
        searchField.setForeground(BONE);
        searchField.setCaretColor(EMBER);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE), new EmptyBorder(8, 10, 8, 10)));
        searchField.addActionListener(e -> runSearch(searchField.getText()));
        bar.add(searchField, BorderLayout.CENTER);

        JButton searchButton = new JButton("Search");
        searchButton.setBackground(EMBER);
        searchButton.setForeground(Color.BLACK);
        searchButton.setFocusPainted(false);
        searchButton.addActionListener(e -> runSearch(searchField.getText()));
        bar.add(searchButton, BorderLayout.EAST);

        updateNavButtons();
        return bar;
    }

    private JButton navButton(String text, ActionListener action) {
        JButton b = new JButton(text);
        b.setBackground(BG);
        b.setForeground(BONE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createLineBorder(LINE));
        b.addActionListener(action);
        return b;
    }

    private JMenuBar buildMenuBar() {
        JMenuBar menu = new JMenuBar();

        JMenu file = new JMenu("File");
        JMenuItem add = new JMenuItem("Index Document\u2026");
        add.addActionListener(e -> showIndexDialog());
        JMenuItem commit = new JMenuItem("Commit");
        commit.addActionListener(e -> {
            controller.commit();
            setStatus("Committed.");
        });
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> frame.dispose());
        file.add(add);
        file.add(commit);
        file.addSeparator();
        file.add(exit);

        JMenu serverMenu = new JMenu("Server");
        JMenuItem toggle = new JMenuItem("Start local HTTP server");
        toggle.addActionListener(e -> toggleServer(toggle));
        serverMenu.add(toggle);

        JMenu help = new JMenu("Help");
        JMenuItem stats = new JMenuItem("Index Stats");
        stats.addActionListener(e -> showStats());
        help.add(stats);

        menu.add(file);
        menu.add(serverMenu);
        menu.add(help);
        return menu;
    }

    private void toggleServer(JMenuItem item) {
        try {
            if (server == null) {
                server = new ArgusServer(8080, new SearchService(index, "body"));
                server.start();
                item.setText("Stop local HTTP server");
                setStatus("Serving on http://localhost:" + server.port() + "   (UI at /)");
            } else {
                server.stop();
                server = null;
                item.setText("Start local HTTP server");
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
    private JPanel card(Map<String, Object> hit) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE), new EmptyBorder(10, 12, 10, 12)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        Map<String, Object> fields = (Map<String, Object>) hit.getOrDefault("fields", Map.of());
        String titleText = String.valueOf(fields.getOrDefault("title", fields.getOrDefault("id", "(document)")));
        JLabel title = new JLabel(titleText);
        title.setForeground(EMBER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));

        double score = ((Number) hit.getOrDefault("score", 0.0)).doubleValue();
        JLabel meta = new JLabel(String.format("#%s   score %.4f", String.valueOf(hit.get("docId")), score));
        meta.setForeground(DIM);

        String bodyText = String.valueOf(fields.getOrDefault("body", ""));
        JLabel body = new JLabel("<html><body style='width:640px'>" + escape(bodyText) + "</body></html>");
        body.setForeground(BONE);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(title, BorderLayout.WEST);
        top.add(meta, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);

        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
        return card;
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
