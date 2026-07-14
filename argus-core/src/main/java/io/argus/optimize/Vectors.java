package io.argus.optimize;

/** Minimal vector helpers for the optimizers. All methods return fresh arrays; inputs are unchanged. */
final class Vectors {

    private Vectors() {
    }

    static double dot(double[] a, double[] b) {
        double s = 0;
        for (int i = 0; i < a.length; i++) {
            s += a[i] * b[i];
        }
        return s;
    }

    static double norm(double[] a) {
        return Math.sqrt(dot(a, a));
    }

    /** Returns {@code y + a*x}. */
    static double[] axpy(double a, double[] x, double[] y) {
        double[] r = new double[y.length];
        for (int i = 0; i < y.length; i++) {
            r[i] = y[i] + a * x[i];
        }
        return r;
    }

    static double[] subtract(double[] a, double[] b) {
        double[] r = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            r[i] = a[i] - b[i];
        }
        return r;
    }

    static double[] scale(double a, double[] x) {
        double[] r = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            r[i] = a * x[i];
        }
        return r;
    }

    static double[] negate(double[] x) {
        return scale(-1.0, x);
    }
}
