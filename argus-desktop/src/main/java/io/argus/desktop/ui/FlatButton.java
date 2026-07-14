package io.argus.desktop.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionListener;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.border.EmptyBorder;

/**
 * A borderless, self-painted button with hover and pressed states. It renders a rounded highlight
 * (circular for icon buttons, a pill for text) instead of the platform's chrome, so it matches the
 * app's browser-like theme on any Look and Feel.
 */
public final class FlatButton extends JButton {

    /** Highlight shape. */
    public enum Shape {
        CIRCLE, PILL, ROUNDED
    }

    private Shape shape = Shape.CIRCLE;
    private boolean primary;
    private int arc = 10;

    public FlatButton(Icon icon) {
        super(icon);
        init();
    }

    public FlatButton(String text) {
        super(text);
        shape = Shape.PILL;
        init();
    }

    private void init() {
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setForeground(Theme.TEXT);
        setFont(Theme.UI);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(6, 12, 6, 12));
    }

    /** A circular icon button sized for a toolbar. */
    public static FlatButton icon(Icons.Kind kind, String tooltip, ActionListener action) {
        FlatButton b = new FlatButton(Icons.of(kind, 18, Theme.TEXT));
        b.setShape(Shape.CIRCLE);
        b.setBorder(new EmptyBorder(0, 0, 0, 0));
        b.setPreferredSize(new Dimension(34, 34));
        b.setToolTipText(tooltip);
        if (action != null) {
            b.addActionListener(action);
        }
        return b;
    }

    /** A filled ember "primary" pill (e.g. Search / Go). */
    public static FlatButton primary(String text, ActionListener action) {
        FlatButton b = new FlatButton(text);
        b.primary = true;
        b.setForeground(new Color(0x14, 0x11, 0x0e));
        b.setFont(Theme.UI_BOLD);
        b.setBorder(new EmptyBorder(8, 18, 8, 18));
        if (action != null) {
            b.addActionListener(action);
        }
        return b;
    }

    /** A subtle outlined pill used for bookmarks / quick links. */
    public static FlatButton chip(String text, ActionListener action) {
        FlatButton b = new FlatButton(text);
        b.setShape(Shape.PILL);
        b.setForeground(Theme.DIM);
        b.setFont(Theme.UI);
        b.setBorder(new EmptyBorder(4, 12, 4, 12));
        if (action != null) {
            b.addActionListener(action);
        }
        return b;
    }

    public FlatButton setShape(Shape s) {
        this.shape = s;
        return this;
    }

    public FlatButton setArc(int a) {
        this.arc = a;
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.aa(g2);
        int w = getWidth();
        int h = getHeight();
        var model = getModel();

        if (primary) {
            Color bg = Theme.EMBER;
            if (model.isPressed()) {
                bg = Theme.EMBER_DEEP;
            } else if (model.isRollover()) {
                bg = Theme.lighten(Theme.EMBER, 0.10);
            }
            g2.setColor(bg);
            fill(g2, w, h);
        } else if (model.isPressed()) {
            g2.setColor(Theme.overlay(34));
            fill(g2, w, h);
        } else if (model.isRollover() && isEnabled()) {
            g2.setColor(Theme.overlay(20));
            fill(g2, w, h);
        }
        g2.dispose();
        super.paintComponent(g);
    }

    private void fill(Graphics2D g2, int w, int h) {
        switch (shape) {
            case CIRCLE -> {
                int d = Math.min(w, h);
                g2.fillOval((w - d) / 2, (h - d) / 2, d, d);
            }
            case PILL -> g2.fillRoundRect(0, 0, w, h, h, h);
            case ROUNDED -> g2.fillRoundRect(0, 0, w, h, arc, arc);
            default -> {
                // no-op
            }
        }
    }
}
