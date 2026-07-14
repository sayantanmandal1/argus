package io.argus.optimize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CoordinateAscentTest {

    @Test
    void maximizesConcaveQuadraticWithoutGradients() {
        // f(x) = -((x0 - 2)^2 + (x1 - 5)^2), maximum 0 at (2, 5)
        ObjectiveFunction f = x -> -((x[0] - 2) * (x[0] - 2) + (x[1] - 5) * (x[1] - 5));
        OptimizationResult r = new CoordinateAscent().maximize(f, new double[] {0, 0});
        assertEquals(2.0, r.point()[0], 1e-3);
        assertEquals(5.0, r.point()[1], 1e-3);
        assertEquals(0.0, r.value(), 1e-5);
    }

    @Test
    void maximizesOneDimensionalObjective() {
        ObjectiveFunction f = x -> -(x[0] - 7) * (x[0] - 7);
        OptimizationResult r = new CoordinateAscent().maximize(f, new double[] {0});
        assertEquals(7.0, r.point()[0], 1e-3);
    }

    @Test
    void improvesFromStartingPoint() {
        // a non-smooth objective (max of two ridges) that gradient methods can't easily handle
        ObjectiveFunction f = x -> Math.min(3 - Math.abs(x[0] - 1), 3 - Math.abs(x[0] - 1)) + 0.0;
        double start = f.value(new double[] {-4});
        OptimizationResult r = new CoordinateAscent().maximize(f, new double[] {-4});
        assertTrue(r.value() > start);
        assertEquals(1.0, r.point()[0], 1e-2);
    }
}
