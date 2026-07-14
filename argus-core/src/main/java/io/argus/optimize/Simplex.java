package io.argus.optimize;

import java.util.Arrays;

/**
 * A two-phase primal simplex solver for {@link LinearProgram}s over non-negative variables.
 *
 * <p>Constraints are converted to standard form with slack, surplus, and artificial variables. Phase
 * one minimizes the sum of artificials to find a feasible basis (or proves infeasibility); phase two
 * optimizes the real objective. Pivoting uses <b>Bland's rule</b> (smallest-index entering and
 * leaving variables) which guarantees termination even on degenerate problems.
 */
public final class Simplex {

    private static final double EPS = 1e-9;
    private static final int MAX_ITERATIONS = 20_000;

    public SimplexResult solve(LinearProgram lp) {
        int n = lp.numVars();
        int m = lp.constraintCount();

        double[][] rows = new double[m][];
        double[] b = new double[m];
        Relation[] rel = new Relation[m];
        for (int i = 0; i < m; i++) {
            rows[i] = lp.lhs(i).clone();
            b[i] = lp.rhs(i);
            rel[i] = lp.relation(i);
            if (b[i] < 0) { // keep right-hand sides non-negative
                for (int j = 0; j < n; j++) {
                    rows[i][j] = -rows[i][j];
                }
                b[i] = -b[i];
                rel[i] = rel[i] == Relation.LE ? Relation.GE : rel[i] == Relation.GE ? Relation.LE : Relation.EQ;
            }
        }

        int slackCount = 0;
        int artificialCount = 0;
        for (Relation r : rel) {
            if (r == Relation.LE) {
                slackCount++;
            } else if (r == Relation.GE) {
                slackCount++;
                artificialCount++;
            } else {
                artificialCount++;
            }
        }

        int total = n + slackCount + artificialCount;
        double[][] a = new double[m][total];
        int[] basis = new int[m];
        boolean[] artificial = new boolean[total];

        int slackCol = n;
        int artCol = n + slackCount;
        for (int i = 0; i < m; i++) {
            System.arraycopy(rows[i], 0, a[i], 0, n);
            switch (rel[i]) {
                case LE -> {
                    a[i][slackCol] = 1.0;
                    basis[i] = slackCol++;
                }
                case GE -> {
                    a[i][slackCol++] = -1.0; // surplus
                    a[i][artCol] = 1.0;
                    artificial[artCol] = true;
                    basis[i] = artCol++;
                }
                case EQ -> {
                    a[i][artCol] = 1.0;
                    artificial[artCol] = true;
                    basis[i] = artCol++;
                }
                default -> throw new IllegalStateException();
            }
        }

        if (artificialCount > 0) {
            double[] phaseOneCost = new double[total];
            for (int j = 0; j < total; j++) {
                if (artificial[j]) {
                    phaseOneCost[j] = -1.0; // maximize -sum(artificials) == minimize sum(artificials)
                }
            }
            boolean[] allowAll = new boolean[total];
            Arrays.fill(allowAll, true);
            run(a, b, basis, phaseOneCost, allowAll);

            double infeasibility = 0;
            for (int i = 0; i < m; i++) {
                infeasibility += phaseOneCost[basis[i]] * b[i];
            }
            if (infeasibility < -EPS) { // some artificial remained positive
                return new SimplexResult(SimplexResult.Status.INFEASIBLE, null, 0);
            }
        }

        double sign = lp.sense() == LinearProgram.Sense.MAXIMIZE ? 1.0 : -1.0;
        double[] cost = new double[total];
        for (int j = 0; j < n; j++) {
            cost[j] = sign * lp.objectiveCoefficients()[j];
        }
        boolean[] allowed = new boolean[total];
        for (int j = 0; j < total; j++) {
            allowed[j] = !artificial[j]; // artificials may not re-enter
        }
        Status phaseTwo = run(a, b, basis, cost, allowed);
        if (phaseTwo == Status.UNBOUNDED) {
            return new SimplexResult(SimplexResult.Status.UNBOUNDED, null, 0);
        }

        double[] x = new double[n];
        for (int i = 0; i < m; i++) {
            if (basis[i] < n) {
                x[basis[i]] = b[i];
            }
        }
        double obj = 0;
        for (int j = 0; j < n; j++) {
            obj += lp.objectiveCoefficients()[j] * x[j];
        }
        return new SimplexResult(SimplexResult.Status.OPTIMAL, x, obj);
    }

    private enum Status {
        OPTIMAL,
        UNBOUNDED
    }

    private Status run(double[][] a, double[] b, int[] basis, double[] cost, boolean[] allowed) {
        int m = a.length;
        int total = cost.length;
        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            // reduced costs relative to the current basis
            double[] reduced = cost.clone();
            for (int i = 0; i < m; i++) {
                double cb = cost[basis[i]];
                if (cb != 0) {
                    for (int j = 0; j < total; j++) {
                        reduced[j] -= cb * a[i][j];
                    }
                }
            }
            int entering = -1;
            for (int j = 0; j < total; j++) {
                if (allowed[j] && reduced[j] > EPS && !isBasic(j, basis)) {
                    entering = j; // Bland: first eligible index
                    break;
                }
            }
            if (entering < 0) {
                return Status.OPTIMAL;
            }
            int leaving = -1;
            double best = Double.POSITIVE_INFINITY;
            for (int i = 0; i < m; i++) {
                if (a[i][entering] > EPS) {
                    double ratio = b[i] / a[i][entering];
                    if (ratio < best - EPS
                            || (Math.abs(ratio - best) <= EPS && (leaving < 0 || basis[i] < basis[leaving]))) {
                        best = ratio;
                        leaving = i;
                    }
                }
            }
            if (leaving < 0) {
                return Status.UNBOUNDED;
            }
            pivot(a, b, leaving, entering);
            basis[leaving] = entering;
        }
        return Status.OPTIMAL;
    }

    private static void pivot(double[][] a, double[] b, int row, int col) {
        int total = a[row].length;
        double p = a[row][col];
        for (int j = 0; j < total; j++) {
            a[row][j] /= p;
        }
        b[row] /= p;
        for (int i = 0; i < a.length; i++) {
            if (i != row) {
                double factor = a[i][col];
                if (factor != 0) {
                    for (int j = 0; j < total; j++) {
                        a[i][j] -= factor * a[row][j];
                    }
                    b[i] -= factor * b[row];
                }
            }
        }
    }

    private static boolean isBasic(int var, int[] basis) {
        for (int bvar : basis) {
            if (bvar == var) {
                return true;
            }
        }
        return false;
    }
}
