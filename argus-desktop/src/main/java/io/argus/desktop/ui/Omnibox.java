package io.argus.desktop.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

/**
 * A rounded "omnibox" address/search bar: a pill-shaped field with a leading status icon (a search
 * glyph, an insecure globe, or a green padlock) and a focus ring in the accent colour. The inner
 * {@link JTextField} is exposed so callers can wire actions and read text.
 */
public final class Omnibox extends JPanel {

    /** The leading indicator shown at the start of the bar. */
    public enum Security {
        SEARCH, INSECURE, SECURE
    }

    private final JTextField field = new JTextField();
    private final JLabel lead = new JLabel();
    private boolean focused;

    public Omnibox(String placeholder) {
        super(new BorderLayout(8, 0));
        setOpaque(false);
        setBorder(new EmptyBorder(0, 14, 0, 12));

        lead.setBorder(new EmptyBorder(0, 0, 0, 0));
        setSecurity(Security.SEARCH);
        add(lead, BorderLayout.WEST);

        field.setOpaque(false);
        field.setBorder(null);
        field.setForeground(Theme.TEXT);
        field.setCaretColor(Theme.EMBER);
        field.setSelectionColor(Theme.lighten(Theme.EMBER, 0.2));
        field.setSelectedTextColor(new Color(0x14, 0x11, 0x0e));
        field.setFont(Theme.uiFont(java.awt.Font.PLAIN, 14f));
        field.setToolTipText(placeholder);
        add(field, BorderLayout.CENTER);

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                focused = true;
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                focused = false;
                repaint();
            }
        });
    }

    public JTextField field() {
        return field;
    }

    public void setSecurity(Security s) {
        Icons.Kind kind = switch (s) {
            case SECURE -> Icons.Kind.LOCK;
            case INSECURE -> Icons.Kind.GLOBE;
            case SEARCH -> Icons.Kind.SEARCH;
        };
        Color c = s == Security.SECURE ? Theme.GREEN : Theme.DIM;
        lead.setIcon(Icons.of(kind, 15, c));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.aa(g2);
        int w = getWidth();
        int h = getHeight();
        g2.setColor(Theme.FIELD);
        g2.fillRoundRect(0, 0, w, h, h, h);
        g2.setStroke(new BasicStroke(focused ? 1.8f : 1f));
        g2.setColor(focused ? Theme.EMBER : Theme.LINE);
        g2.drawRoundRect(1, 1, w - 2, h - 2, h - 2, h - 2);
        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        d.height = Math.max(d.height, 38);
        return d;
    }
}
