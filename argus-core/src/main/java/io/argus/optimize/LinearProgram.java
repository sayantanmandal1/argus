package io.argus.optimize;

import java.util.ArrayList;
import java.util.List;

/**
 * A linear program over non-negative variables: an objective {@code c·x} to maximize or minimize,
 * subject to linear constraints {@code a·x {<=,>=,==} b}. Built fluently, then handed to
 * {@link Simplex}.
 *
 * <pre>
 *   LinearProgram lp = new LinearProgram(2)
 *       .maximize(new double[]{3, 5})
 *       .subjectTo(new double[]{1, 0}, Relation.LE, 4)
 *       .subjectTo(new double[]{0, 2}, Relation.LE, 12)
 *       .subjectTo(new double[]{3, 2}, Relation.LE, 18);
 * </pre>
 */
public final class LinearProgram {

    /** Whether the objective is maximized or minimized. */
    public enum Sense {
        MAXIMIZE,
        MINIMIZE
    }

    private final int numVars;
    private Sense sense = Sense.MAXIMIZE;
    private double[] objective;
    private final List<double[]> lhs = new ArrayList<>();
    private final List<Relation> relations = new ArrayList<>();
    private final List<Double> rhs = new ArrayList<>();

    public LinearProgram(int numVars) {
        if (numVars <= 0) {
            throw new IllegalArgumentException("numVars must be > 0");
        }
        this.numVars = numVars;
        this.objective = new double[numVars];
    }

    public LinearProgram maximize(double[] c) {
        return objective(Sense.MAXIMIZE, c);
    }

    public LinearProgram minimize(double[] c) {
        return objective(Sense.MINIMIZE, c);
    }

    private LinearProgram objective(Sense s, double[] c) {
        if (c.length != numVars) {
            throw new IllegalArgumentException("objective length " + c.length + " != " + numVars);
        }
        this.sense = s;
        this.objective = c.clone();
        return this;
    }

    public LinearProgram subjectTo(double[] a, Relation relation, double b) {
        if (a.length != numVars) {
            throw new IllegalArgumentException("constraint length " + a.length + " != " + numVars);
        }
        lhs.add(a.clone());
        relations.add(relation);
        rhs.add(b);
        return this;
    }

    public int numVars() {
        return numVars;
    }

    public int constraintCount() {
        return lhs.size();
    }

    public Sense sense() {
        return sense;
    }

    public double[] objectiveCoefficients() {
        return objective;
    }

    public double[] lhs(int i) {
        return lhs.get(i);
    }

    public Relation relation(int i) {
        return relations.get(i);
    }

    public double rhs(int i) {
        return rhs.get(i);
    }
}
