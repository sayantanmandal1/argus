package io.argus.optimize;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Limited-memory BFGS — a quasi-Newton method that approximates the inverse Hessian from the last
 * {@code m} gradient/step pairs via the two-loop recursion, giving near-Newton convergence with only
 * O(m·n) memory. Uses an Armijo backtracking line search and falls back to steepest descent if a
 * proposed direction is not a descent direction. Handles ill-conditioned problems (e.g. Rosenbrock)
 * that defeat plain gradient descent.
 */
public final class LBFGS {

    private final int history;
    private final int maxIterations;
    private final double tolerance;

    public LBFGS() {
        this(10, 1_000, 1e-8);
    }

    public LBFGS(int history, int maxIterations, double tolerance) {
        this.history = history;
        this.maxIterations = maxIterations;
        this.tolerance = tolerance;
    }

    public OptimizationResult minimize(DifferentiableFunction f, double[] x0) {
        double[] x = x0.clone();
        double[] g = f.gradient(x);
        double fx = f.value(x);

        Deque<double[]> sHist = new ArrayDeque<>();
        Deque<double[]> yHist = new ArrayDeque<>();
        Deque<Double> rhoHist = new ArrayDeque<>();

        for (int iter = 1; iter <= maxIterations; iter++) {
            if (Vectors.norm(g) < tolerance) {
                return new OptimizationResult(x, fx, iter, true);
            }

            double[] direction = twoLoopDirection(g, sHist, yHist, rhoHist);
            double gd = Vectors.dot(g, direction);
            if (gd >= 0) { // not a descent direction; reset
                direction = Vectors.negate(g);
                gd = -Vectors.dot(g, g);
            }

            double t = 1.0;
            double[] candidate = Vectors.axpy(t, direction, x);
            double fCandidate = f.value(candidate);
            while (fCandidate > fx + 1e-4 * t * gd && t > 1e-16) {
                t *= 0.5;
                candidate = Vectors.axpy(t, direction, x);
                fCandidate = f.value(candidate);
            }

            double[] gCandidate = f.gradient(candidate);
            double[] s = Vectors.subtract(candidate, x);
            double[] y = Vectors.subtract(gCandidate, g);
            double sy = Vectors.dot(s, y);
            if (sy > 1e-12) {
                sHist.addLast(s);
                yHist.addLast(y);
                rhoHist.addLast(1.0 / sy);
                if (sHist.size() > history) {
                    sHist.removeFirst();
                    yHist.removeFirst();
                    rhoHist.removeFirst();
                }
            }

            double previous = fx;
            x = candidate;
            g = gCandidate;
            fx = fCandidate;
            if (Math.abs(previous - fx) < tolerance * (1 + Math.abs(fx))) {
                return new OptimizationResult(x, fx, iter, true);
            }
        }
        return new OptimizationResult(x, fx, maxIterations, false);
    }

    private double[] twoLoopDirection(double[] g, Deque<double[]> sHist, Deque<double[]> yHist,
                                      Deque<Double> rhoHist) {
        double[] q = g.clone();
        int k = sHist.size();
        double[] alpha = new double[k];

        java.util.Iterator<double[]> sIt = sHist.descendingIterator();
        java.util.Iterator<double[]> yIt = yHist.descendingIterator();
        java.util.Iterator<Double> rhoIt = rhoHist.descendingIterator();
        int idx = k - 1;
        while (sIt.hasNext()) {
            double[] s = sIt.next();
            double[] y = yIt.next();
            double rho = rhoIt.next();
            double a = rho * Vectors.dot(s, q);
            alpha[idx] = a;
            q = Vectors.axpy(-a, y, q);
            idx--;
        }

        double gamma = 1.0;
        if (!sHist.isEmpty()) {
            double[] sLast = sHist.peekLast();
            double[] yLast = yHist.peekLast();
            gamma = Vectors.dot(sLast, yLast) / Vectors.dot(yLast, yLast);
        }
        double[] r = Vectors.scale(gamma, q);

        java.util.Iterator<double[]> sIt2 = sHist.iterator();
        java.util.Iterator<double[]> yIt2 = yHist.iterator();
        java.util.Iterator<Double> rhoIt2 = rhoHist.iterator();
        idx = 0;
        while (sIt2.hasNext()) {
            double[] s = sIt2.next();
            double[] y = yIt2.next();
            double rho = rhoIt2.next();
            double beta = rho * Vectors.dot(y, r);
            r = Vectors.axpy(alpha[idx] - beta, s, r);
            idx++;
        }
        return Vectors.negate(r);
    }
}
