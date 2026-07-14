package io.argus.desktop.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.Icon;
import javax.swing.JComponent;

/**
 * Crisp, dependency-free vector icons painted with Java2D. Each icon scales to any size and dims
 * automatically when its host component is disabled, so toolbar buttons look right in every state.
 */
public final class Icons {

    /** The available glyphs. */
    public enum Kind {
        BACK, FORWARD, RELOAD, STOP, HOME, LOCK, GLOBE, SEARCH, PLUS, CLOSE, MENU, STAR
    }

    private Icons() {
    }

    public static Icon of(Kind kind, int size, Color color) {
        return new VectorIcon(kind, size, color, Math.max(1.4f, size * 0.09f));
    }

    public static Icon of(Kind kind, int size, Color color, float stroke) {
        return new VectorIcon(kind, size, color, stroke);
    }

    private static final class VectorIcon implements Icon {

        private final Kind kind;
        private final int size;
        private final Color color;
        private final float stroke;

        VectorIcon(Kind kind, int size, Color color, float stroke) {
            this.kind = kind;
            this.size = size;
            this.color = color;
            this.stroke = stroke;
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.aa(g2);
            g2.translate(x, y);
            boolean enabled = !(c instanceof JComponent) || c.isEnabled();
            Color paint = enabled ? color : new Color(color.getRed(), color.getGreen(), color.getBlue(), 90);
            g2.setColor(paint);
            g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            double s = size;
            double p = s * 0.24;      // padding
            double a = p;
            double b = s - p;
            double mid = s / 2.0;

            switch (kind) {
                case BACK -> chevronArrow(g2, a, b, mid, true);
                case FORWARD -> chevronArrow(g2, a, b, mid, false);
                case RELOAD -> reload(g2, s, mid);
                case STOP -> cross(g2, a, b);
                case CLOSE -> cross(g2, s * 0.3, s * 0.7);
                case HOME -> home(g2, s, mid);
                case LOCK -> lock(g2, s, mid);
                case GLOBE -> globe(g2, s, mid);
                case SEARCH -> search(g2, s);
                case PLUS -> plus(g2, a, b, mid);
                case MENU -> menu(g2, s, mid);
                case STAR -> star(g2, s, mid);
                default -> {
                    // no-op
                }
            }
            g2.dispose();
        }

        private void chevronArrow(Graphics2D g2, double a, double b, double mid, boolean left) {
            double head = (b - a) * 0.42;
            GeneralPath shaft = new GeneralPath();
            shaft.moveTo(a, mid);
            shaft.lineTo(b, mid);
            g2.draw(shaft);
            GeneralPath tip = new GeneralPath();
            if (left) {
                tip.moveTo(a + head, mid - head);
                tip.lineTo(a, mid);
                tip.lineTo(a + head, mid + head);
            } else {
                tip.moveTo(b - head, mid - head);
                tip.lineTo(b, mid);
                tip.lineTo(b - head, mid + head);
            }
            g2.draw(tip);
        }

        private void reload(Graphics2D g2, double s, double mid) {
            double r = s * 0.28;
            g2.draw(new Arc2D.Double(mid - r, mid - r, r * 2, r * 2, 70, 250, Arc2D.OPEN));
            // arrowhead at the arc's start (~70 degrees, top-right)
            double ang = Math.toRadians(70);
            double ex = mid + r * Math.cos(ang);
            double ey = mid - r * Math.sin(ang);
            double h = s * 0.16;
            GeneralPath head = new GeneralPath();
            head.moveTo(ex - h, ey - h * 0.2);
            head.lineTo(ex, ey);
            head.lineTo(ex + h * 0.1, ey - h);
            g2.draw(head);
        }

        private void cross(Graphics2D g2, double a, double b) {
            GeneralPath p = new GeneralPath();
            p.moveTo(a, a);
            p.lineTo(b, b);
            p.moveTo(b, a);
            p.lineTo(a, b);
            g2.draw(p);
        }

        private void plus(Graphics2D g2, double a, double b, double mid) {
            GeneralPath p = new GeneralPath();
            p.moveTo(mid, a);
            p.lineTo(mid, b);
            p.moveTo(a, mid);
            p.lineTo(b, mid);
            g2.draw(p);
        }

        private void home(Graphics2D g2, double s, double mid) {
            double roofY = s * 0.24;
            double eaveY = s * 0.46;
            double left = s * 0.24;
            double right = s * 0.76;
            double bottom = s * 0.76;
            GeneralPath roof = new GeneralPath();
            roof.moveTo(left - s * 0.04, eaveY + s * 0.02);
            roof.lineTo(mid, roofY);
            roof.lineTo(right + s * 0.04, eaveY + s * 0.02);
            g2.draw(roof);
            double bodyLeft = s * 0.31;
            double bodyRight = s * 0.69;
            g2.draw(new RoundRectangle2D.Double(bodyLeft, eaveY, bodyRight - bodyLeft, bottom - eaveY,
                    s * 0.06, s * 0.06));
        }

        private void lock(Graphics2D g2, double s, double mid) {
            double bodyW = s * 0.42;
            double bodyH = s * 0.30;
            double bodyX = mid - bodyW / 2;
            double bodyY = s * 0.46;
            g2.fill(new RoundRectangle2D.Double(bodyX, bodyY, bodyW, bodyH, s * 0.10, s * 0.10));
            double shackleR = s * 0.14;
            g2.draw(new Arc2D.Double(mid - shackleR, bodyY - shackleR * 1.5, shackleR * 2, shackleR * 2,
                    0, 180, Arc2D.OPEN));
            g2.draw(new java.awt.geom.Line2D.Double(mid - shackleR, bodyY - shackleR * 0.5, mid - shackleR, bodyY));
            g2.draw(new java.awt.geom.Line2D.Double(mid + shackleR, bodyY - shackleR * 0.5, mid + shackleR, bodyY));
        }

        private void globe(Graphics2D g2, double s, double mid) {
            double r = s * 0.28;
            g2.draw(new Ellipse2D.Double(mid - r, mid - r, r * 2, r * 2));
            g2.draw(new Ellipse2D.Double(mid - r * 0.45, mid - r, r * 0.9, r * 2));
            g2.draw(new java.awt.geom.Line2D.Double(mid - r, mid, mid + r, mid));
        }

        private void search(Graphics2D g2, double s) {
            double cx = s * 0.42;
            double cy = s * 0.42;
            double r = s * 0.20;
            g2.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
            double hx = cx + r * 0.72;
            double hy = cy + r * 0.72;
            g2.draw(new java.awt.geom.Line2D.Double(hx, hy, s * 0.76, s * 0.76));
        }

        private void menu(Graphics2D g2, double s, double mid) {
            double r = s * 0.06;
            for (double cy : new double[] {s * 0.28, s * 0.5, s * 0.72}) {
                g2.fill(new Ellipse2D.Double(mid - r, cy - r, r * 2, r * 2));
            }
        }

        private void star(Graphics2D g2, double s, double mid) {
            double outer = s * 0.30;
            double inner = outer * 0.42;
            Path2D star = new Path2D.Double();
            for (int i = 0; i < 10; i++) {
                double radius = (i % 2 == 0) ? outer : inner;
                double theta = -Math.PI / 2 + i * Math.PI / 5;
                double px = mid + radius * Math.cos(theta);
                double py = mid + radius * Math.sin(theta);
                if (i == 0) {
                    star.moveTo(px, py);
                } else {
                    star.lineTo(px, py);
                }
            }
            star.closePath();
            g2.draw(star);
        }
    }
}
