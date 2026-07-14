package io.argus.optimize;

import java.util.Arrays;

/** The result of a numerical optimization run. */
public final class OptimizationResult {

    private final double[] point;
    private final double value;
    private final int iterations;
    private final boolean converged;

    public OptimizationResult(double[] point, double value, int iterations, boolean converged) {
        this.point = point.clone();
        this.value = value;
        this.iterations = iterations;
        this.converged = converged;
    }

    /** The best point found. */
    public double[] point() {
        return point.clone();
    }

    /** The objective value at {@link #point()}. */
    public double value() {
        return value;
    }

    public int iterations() {
        return iterations;
    }

    /** Whether the run met its convergence criterion (vs. hitting the iteration cap). */
    public boolean converged() {
        return converged;
    }

    @Override
    public String toString() {
        return "OptimizationResult{x=" + Arrays.toString(point) + ", f=" + value
                + ", iters=" + iterations + ", converged=" + converged + "}";
    }
}
