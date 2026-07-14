package io.argus.optimize;

/** A scalar objective {@code f(x)} over a real vector. */
public interface ObjectiveFunction {

    double value(double[] x);
}
