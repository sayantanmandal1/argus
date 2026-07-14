package io.argus.optimize;

/**
 * Steepest-descent minimization with an Armijo backtracking line search. Simple and robust for
 * well-conditioned problems; {@link LBFGS} is preferred for harder ones.
 */
public final class GradientDescent {

    private final int maxIterations;
    private final double tolerance;

    public GradientDescent() {
        this(10_000, 1e-8);
    }

    public GradientDescent(int maxIterations, double tolerance) {
        this.maxIterations = maxIterations;
        this.tolerance = tolerance;
    }

    public OptimizationResult minimize(DifferentiableFunction f, double[] x0) {
        double[] x = x0.clone();
        double fx = f.value(x);
        for (int iter = 1; iter <= maxIterations; iter++) {
            double[] g = f.gradient(x);
            double gnorm = Vectors.norm(g);
            if (gnorm < tolerance) {
                return new OptimizationResult(x, fx, iter, true);
            }
            double t = 1.0;
            double[] candidate = Vectors.axpy(-t, g, x);
            double fCandidate = f.value(candidate);
            while (fCandidate > fx - 1e-4 * t * gnorm * gnorm && t > 1e-16) {
                t *= 0.5;
                candidate = Vectors.axpy(-t, g, x);
                fCandidate = f.value(candidate);
            }
            boolean tinyProgress = Math.abs(fx - fCandidate) < tolerance * (1 + Math.abs(fx));
            x = candidate;
            fx = fCandidate;
            if (tinyProgress) {
                return new OptimizationResult(x, fx, iter, true);
            }
        }
        return new OptimizationResult(x, fx, maxIterations, false);
    }
}
