package io.argus.optimize;

/**
 * Derivative-free coordinate ascent: repeatedly line-searches along one coordinate at a time,
 * expanding the step while it keeps improving and halving it otherwise. Because it never needs a
 * gradient, it can maximize objectives that are non-differentiable or noisy — which is exactly the
 * situation when tuning search-ranking parameters against a rank metric such as NDCG.
 */
public final class CoordinateAscent {

    private final int maxSweeps;
    private final double tolerance;
    private final double initialStep;

    public CoordinateAscent() {
        this(200, 1e-7, 1.0);
    }

    public CoordinateAscent(int maxSweeps, double tolerance, double initialStep) {
        this.maxSweeps = maxSweeps;
        this.tolerance = tolerance;
        this.initialStep = initialStep;
    }

    public OptimizationResult maximize(ObjectiveFunction f, double[] x0) {
        double[] x = x0.clone();
        double fx = f.value(x);
        int sweeps = 0;

        for (int sweep = 0; sweep < maxSweeps; sweep++) {
            sweeps++;
            double before = fx;
            for (int i = 0; i < x.length; i++) {
                double step = initialStep;
                while (step > tolerance) {
                    boolean improved = false;
                    for (int dir = -1; dir <= 1; dir += 2) {
                        double original = x[i];
                        x[i] = original + dir * step;
                        double candidate = f.value(x);
                        if (candidate > fx + 1e-12) {
                            fx = candidate; // accept the move, keep x[i]
                            improved = true;
                            break;
                        }
                        x[i] = original; // revert
                    }
                    if (!improved) {
                        step *= 0.5;
                    }
                }
            }
            if (Math.abs(fx - before) < tolerance) {
                return new OptimizationResult(x, fx, sweeps, true);
            }
        }
        return new OptimizationResult(x, fx, sweeps, false);
    }
}
