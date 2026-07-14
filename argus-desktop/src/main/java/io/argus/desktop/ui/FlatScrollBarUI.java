package io.argus.desktop.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.plaf.basic.BasicScrollBarUI;

/**
 * A minimal, modern scrollbar: no arrow buttons, a transparent track, and a rounded thumb that
 * brightens on hover. Apply it to a scroll pane with {@link #apply(JScrollPane)}.
 */
public final class FlatScrollBarUI extends BasicScrollBarUI {

    private static final Color THUMB = new Color(0x3a, 0x40, 0x4b);

    /** Installs the flat UI on both scrollbars of a scroll pane and slims them down. */
    public static void apply(JScrollPane pane) {
        pane.getVerticalScrollBar().setUI(new FlatScrollBarUI());
        pane.getHorizontalScrollBar().setUI(new FlatScrollBarUI());
        pane.getVerticalScrollBar().setPreferredSize(new Dimension(11, 0));
        pane.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 11));
        pane.getVerticalScrollBar().setOpaque(false);
        pane.getHorizontalScrollBar().setOpaque(false);
    }

    @Override
    protected void configureScrollBarColors() {
        // Colours are painted directly in paintThumb.
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return zeroButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return zeroButton();
    }

    private JButton zeroButton() {
        JButton b = new JButton();
        Dimension zero = new Dimension(0, 0);
        b.setPreferredSize(zero);
        b.setMinimumSize(zero);
        b.setMaximumSize(zero);
        return b;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        // Transparent track.
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
        if (r.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.aa(g2);
        Color color = isThumbRollover() ? Theme.DIM : THUMB;
        g2.setColor(color);
        int arc = Math.min(r.width, r.height);
        g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, arc, arc);
        g2.dispose();
    }
}
