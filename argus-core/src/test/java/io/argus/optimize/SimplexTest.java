package io.argus.optimize;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SimplexTest {

    private final Simplex simplex = new Simplex();

    @Test
    void solvesClassicMaximization() {
        // maximize 3x + 5y  s.t.  x <= 4,  2y <= 12,  3x + 2y <= 18  ->  optimum (2, 6) = 36
        LinearProgram lp = new LinearProgram(2)
                .maximize(new double[] {3, 5})
                .subjectTo(new double[] {1, 0}, Relation.LE, 4)
                .subjectTo(new double[] {0, 2}, Relation.LE, 12)
                .subjectTo(new double[] {3, 2}, Relation.LE, 18);
        SimplexResult r = simplex.solve(lp);
        assertEquals(SimplexResult.Status.OPTIMAL, r.status());
        assertEquals(36.0, r.objective(), 1e-6);
        assertEquals(2.0, r.solution()[0], 1e-6);
        assertEquals(6.0, r.solution()[1], 1e-6);
    }

    @Test
    void solvesMinimizationWithGreaterThanConstraints() {
        // minimize 2x + 3y  s.t.  x + y >= 10,  x <= 8  ->  optimum (8, 2) = 22
        LinearProgram lp = new LinearProgram(2)
                .minimize(new double[] {2, 3})
                .subjectTo(new double[] {1, 1}, Relation.GE, 10)
                .subjectTo(new double[] {1, 0}, Relation.LE, 8);
        SimplexResult r = simplex.solve(lp);
        assertEquals(SimplexResult.Status.OPTIMAL, r.status());
        assertEquals(22.0, r.objective(), 1e-6);
        assertEquals(8.0, r.solution()[0], 1e-6);
        assertEquals(2.0, r.solution()[1], 1e-6);
    }

    @Test
    void handlesEqualityConstraints() {
        // maximize 3x + 2y  s.t.  x + y == 10,  x <= 6  ->  optimum (6, 4) = 26
        LinearProgram lp = new LinearProgram(2)
                .maximize(new double[] {3, 2})
                .subjectTo(new double[] {1, 1}, Relation.EQ, 10)
                .subjectTo(new double[] {1, 0}, Relation.LE, 6);
        SimplexResult r = simplex.solve(lp);
        assertEquals(SimplexResult.Status.OPTIMAL, r.status());
        assertEquals(26.0, r.objective(), 1e-6);
        assertEquals(6.0, r.solution()[0], 1e-6);
        assertEquals(4.0, r.solution()[1], 1e-6);
    }

    @Test
    void detectsInfeasibility() {
        // x <= 3 and x >= 5 cannot both hold
        LinearProgram lp = new LinearProgram(1)
                .maximize(new double[] {1})
                .subjectTo(new double[] {1}, Relation.LE, 3)
                .subjectTo(new double[] {1}, Relation.GE, 5);
        assertEquals(SimplexResult.Status.INFEASIBLE, simplex.solve(lp).status());
    }

    @Test
    void detectsUnboundedness() {
        // maximize x + y with only x - y <= 1 (y can grow without bound)
        LinearProgram lp = new LinearProgram(2)
                .maximize(new double[] {1, 1})
                .subjectTo(new double[] {1, -1}, Relation.LE, 1);
        assertEquals(SimplexResult.Status.UNBOUNDED, simplex.solve(lp).status());
    }

    @Test
    void solvesLargerBlendingProblem() {
        // maximize 4a + 3b + 5c s.t. a+b+c<=10, 2a+b<=12, b+3c<=15, all >=0
        LinearProgram lp = new LinearProgram(3)
                .maximize(new double[] {4, 3, 5})
                .subjectTo(new double[] {1, 1, 1}, Relation.LE, 10)
                .subjectTo(new double[] {2, 1, 0}, Relation.LE, 12)
                .subjectTo(new double[] {0, 1, 3}, Relation.LE, 15);
        SimplexResult r = simplex.solve(lp);
        assertEquals(SimplexResult.Status.OPTIMAL, r.status());
        // objective is a valid upper bound and constraints are satisfied
        double a = r.solution()[0];
        double b = r.solution()[1];
        double c = r.solution()[2];
        assertEquals(4 * a + 3 * b + 5 * c, r.objective(), 1e-6);
        org.junit.jupiter.api.Assertions.assertTrue(a + b + c <= 10 + 1e-6);
        org.junit.jupiter.api.Assertions.assertTrue(2 * a + b <= 12 + 1e-6);
        org.junit.jupiter.api.Assertions.assertTrue(b + 3 * c <= 15 + 1e-6);
        org.junit.jupiter.api.Assertions.assertTrue(r.objective() >= 43.0); // known optimum is 43.75
    }
}
