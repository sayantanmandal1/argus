package io.argus.optimize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NonlinearOptimizationTest {

    /** f(x) = (x0 - 3)^2 + (x1 + 1)^2, minimum 0 at (3, -1). */
    private static final DifferentiableFunction QUADRATIC = new DifferentiableFunction() {
        @Override
        public double value(double[] x) {
            return (x[0] - 3) * (x[0] - 3) + (x[1] + 1) * (x[1] + 1);
        }

        @Override
        public double[] gradient(double[] x) {
            return new double[] {2 * (x[0] - 3), 2 * (x[1] + 1)};
        }
    };

    /** Rosenbrock: f(x,y) = (1-x)^2 + 100(y - x^2)^2, minimum 0 at (1, 1). */
    private static final DifferentiableFunction ROSENBROCK = new DifferentiableFunction() {
        @Override
        public double value(double[] v) {
            double x = v[0];
            double y = v[1];
            return (1 - x) * (1 - x) + 100 * (y - x * x) * (y - x * x);
        }

        @Override
        public double[] gradient(double[] v) {
            double x = v[0];
            double y = v[1];
            return new double[] {
                    -2 * (1 - x) - 400 * x * (y - x * x),
                    200 * (y - x * x)
            };
        }
    };

    @Test
    void gradientDescentMinimizesQuadratic() {
        OptimizationResult r = new GradientDescent().minimize(QUADRATIC, new double[] {0, 0});
        assertTrue(r.converged());
        assertEquals(3.0, r.point()[0], 1e-3);
        assertEquals(-1.0, r.point()[1], 1e-3);
        assertEquals(0.0, r.value(), 1e-6);
    }

    @Test
    void lbfgsMinimizesQuadratic() {
        OptimizationResult r = new LBFGS().minimize(QUADRATIC, new double[] {-5, 5});
        assertEquals(3.0, r.point()[0], 1e-4);
        assertEquals(-1.0, r.point()[1], 1e-4);
        assertEquals(0.0, r.value(), 1e-8);
    }

    @Test
    void lbfgsSolvesRosenbrockWhereGradientDescentStruggles() {
        OptimizationResult r = new LBFGS(10, 2000, 1e-10).minimize(ROSENBROCK, new double[] {-1.2, 1.0});
        assertEquals(1.0, r.point()[0], 1e-2);
        assertEquals(1.0, r.point()[1], 1e-2);
        assertTrue(r.value() < 1e-4, "expected near-zero minimum, got " + r.value());
    }
}
