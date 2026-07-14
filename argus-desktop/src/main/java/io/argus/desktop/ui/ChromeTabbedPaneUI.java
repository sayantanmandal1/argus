package io.argus.desktop.ui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import javax.swing.JTabbedPane;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

/**
 * A flat, Chrome-like skin for {@link JTabbedPane}: a dark tab strip, a raised rounded background
 * for the active tab that merges into the content below, a soft hover highlight, and no content
 * border. Titles and close buttons are supplied by {@link TabHeader} tab components.
 */
public final class ChromeTabbedPaneUI extends BasicTabbedPaneUI {

    private static final int ARC = 12;

    /** Installs the UI and browser-like defaults on a tabbed pane. */
    public static void install(JTabbedPane pane) {
        pane.setUI(new ChromeTabbedPaneUI());
        pane.setBackground(Theme.STRIP);
        pane.setForeground(Theme.DIM);
        pane.setFont(Theme.UI);
        pane.setFocusable(false);
        pane.setOpaque(true);
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        tabInsets = new Insets(7, 14, 7, 10);
        selectedTabPadInsets = new Insets(0, 0, 0, 0);
        tabAreaInsets = new Insets(6, 8, 0, 8);
        contentBorderInsets = new Insets(0, 0, 0, 0);
    }

    @Override
    protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight) {
        return Math.max(36, super.calculateTabHeight(tabPlacement, tabIndex, fontHeight));
    }

    @Override
    protected int getTabRunOverlay(int tabPlacement) {
        return 0;
    }

    @Override
    protected void paintTabArea(Graphics g, int tabPlacement, int selectedIndex) {
        int areaHeight = calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
        g.setColor(Theme.STRIP);
        g.fillRect(0, 0, tabPane.getWidth(), areaHeight + tabAreaInsets.top + 2);
        super.paintTabArea(g, tabPlacement, selectedIndex);
    }

    @Override
    protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                                      int x, int y, int w, int h, boolean isSelected) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.aa(g2);
        if (isSelected) {
            g2.setColor(Theme.CHROME);
            g2.fillRoundRect(x + 1, y + 3, w - 2, h + ARC, ARC, ARC);
        } else if (getRolloverTab() == tabIndex) {
            g2.setColor(Theme.overlay(14));
            g2.fillRoundRect(x + 3, y + 5, w - 6, h - 4, ARC, ARC);
        }
        g2.dispose();
    }

    @Override
    protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
                                  int x, int y, int w, int h, boolean isSelected) {
        // No per-tab border; the raised background carries the shape.
    }

    @Override
    protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
        // Seamless: content panels paint their own background.
    }

    @Override
    protected void paintFocusIndicator(Graphics g, int tabPlacement, java.awt.Rectangle[] rects,
                                       int tabIndex, java.awt.Rectangle iconRect,
                                       java.awt.Rectangle textRect, boolean isSelected) {
        // No focus ring.
    }
}
