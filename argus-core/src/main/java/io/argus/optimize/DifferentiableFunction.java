package io.argus.optimize;

/** An objective that also exposes its gradient, enabling {@link GradientDescent} and {@link LBFGS}. */
public interface DifferentiableFunction extends ObjectiveFunction {

    double[] gradient(double[] x);
}
