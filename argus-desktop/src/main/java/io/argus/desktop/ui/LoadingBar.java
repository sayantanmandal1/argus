package io.argus.desktop.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * A slim indeterminate progress strip that slides an ember segment across while a page loads,
 * mirroring the loading bar of a real browser. It paints nothing when idle.
 */
public final class LoadingBar extends JComponent {

    private final Timer timer;
    private double pos = -0.35;
    private boolean active;

    public LoadingBar() {
        setPreferredSize(new Dimension(10, 3));
        timer = new Timer(16, e -> {
            pos += 0.02;
            if (pos > 1.0) {
                pos = -0.35;
            }
            repaint();
        });
        timer.setCoalesce(true);
    }

    public void start() {
        active = true;
        pos = -0.35;
        timer.start();
        repaint();
    }

    public void stop() {
        active = false;
        timer.stop();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (!active) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.aa(g2);
        int w = getWidth();
        int h = getHeight();
        int segW = Math.max(60, (int) (w * 0.30));
        int x = (int) (pos * (w + segW)) - segW;
        g2.setColor(Theme.EMBER);
        g2.fillRoundRect(x, 0, segW, h, h, h);
        g2.dispose();
    }
}
