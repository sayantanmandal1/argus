package io.argus.optimize;

import java.util.Arrays;

/** The outcome of solving a {@link LinearProgram}: a status and, when optimal, the solution. */
public final class SimplexResult {

    public enum Status {
        OPTIMAL,
        INFEASIBLE,
        UNBOUNDED
    }

    private final Status status;
    private final double[] solution;
    private final double objective;

    SimplexResult(Status status, double[] solution, double objective) {
        this.status = status;
        this.solution = solution;
        this.objective = objective;
    }

    public Status status() {
        return status;
    }

    public boolean isOptimal() {
        return status == Status.OPTIMAL;
    }

    /** The optimal variable values, or {@code null} if not optimal. */
    public double[] solution() {
        return solution == null ? null : solution.clone();
    }

    /** The optimal objective value (meaningful only when {@link #isOptimal()}). */
    public double objective() {
        return objective;
    }

    @Override
    public String toString() {
        return "SimplexResult{" + status
                + (solution == null ? "" : ", x=" + Arrays.toString(solution) + ", obj=" + objective) + "}";
    }
}
