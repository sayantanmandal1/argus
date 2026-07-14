package io.argus.desktop.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * The component shown on a browser tab: an optional favicon, a truncated title, and (for closable
 * tabs) a round close button. Transparent, so the Chrome-style tab background painted by
 * {@link ChromeTabbedPaneUI} shows through.
 */
public final class TabHeader extends JPanel {

    private static final int MAX_CHARS = 22;

    private final JLabel favicon = new JLabel();
    private final JLabel title = new JLabel();
    private String fullTitle = "";

    public TabHeader(String text, Icon icon, Runnable onClose) {
        super(new BorderLayout(7, 0));
        setOpaque(false);
        setBorder(new EmptyBorder(0, 2, 0, 0));

        favicon.setIcon(icon);
        add(favicon, BorderLayout.WEST);

        title.setForeground(Theme.TEXT);
        title.setFont(Theme.UI);
        add(title, BorderLayout.CENTER);
        setTitle(text);

        if (onClose != null) {
            FlatButton close = new FlatButton(Icons.of(Icons.Kind.CLOSE, 12, Theme.DIM));
            close.setShape(FlatButton.Shape.CIRCLE);
            close.setBorder(new EmptyBorder(0, 0, 0, 0));
            close.setPreferredSize(new Dimension(18, 18));
            close.setToolTipText("Close tab");
            close.addActionListener(e -> onClose.run());
            JPanel east = new JPanel(new BorderLayout());
            east.setOpaque(false);
            east.setBorder(new EmptyBorder(0, 6, 0, 0));
            east.add(close, BorderLayout.CENTER);
            add(east, BorderLayout.EAST);
        }
    }

    public void setTitle(String text) {
        fullTitle = text == null || text.isBlank() ? "New Tab" : text;
        String shown = fullTitle.length() > MAX_CHARS ? fullTitle.substring(0, MAX_CHARS - 1) + "\u2026" : fullTitle;
        title.setText(shown);
        setToolTipText(fullTitle);
    }

    public void setFavicon(Icon icon) {
        favicon.setIcon(icon);
    }

    public void setSelected(boolean selected) {
        title.setForeground(selected ? Theme.TEXT : Theme.DIM);
        title.setFont(selected ? Theme.UI_BOLD : Theme.UI);
    }
}
