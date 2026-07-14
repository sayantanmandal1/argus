package io.argus.browser.js;

import io.argus.browser.js.Ast.ArrayLit;
import io.argus.browser.js.Ast.Assign;
import io.argus.browser.js.Ast.Binary;
import io.argus.browser.js.Ast.Block;
import io.argus.browser.js.Ast.BoolLit;
import io.argus.browser.js.Ast.Call;
import io.argus.browser.js.Ast.Conditional;
import io.argus.browser.js.Ast.Declarator;
import io.argus.browser.js.Ast.Expr;
import io.argus.browser.js.Ast.ExprStmt;
import io.argus.browser.js.Ast.ForEach;
import io.argus.browser.js.Ast.FunctionDecl;
import io.argus.browser.js.Ast.FunctionExpr;
import io.argus.browser.js.Ast.Identifier;
import io.argus.browser.js.Ast.Logical;
import io.argus.browser.js.Ast.Member;
import io.argus.browser.js.Ast.NewExpr;
import io.argus.browser.js.Ast.NumberLit;
import io.argus.browser.js.Ast.ObjectLit;
import io.argus.browser.js.Ast.Program;
import io.argus.browser.js.Ast.Property;
import io.argus.browser.js.Ast.Stmt;
import io.argus.browser.js.Ast.StringLit;
import io.argus.browser.js.Ast.ThisExpr;
import io.argus.browser.js.Ast.Unary;
import io.argus.browser.js.Ast.UndefinedLit;
import io.argus.browser.js.Ast.Update;
import io.argus.browser.js.Ast.VarDecl;
import java.util.ArrayList;
import java.util.List;

/**
 * A tree-walking interpreter for the JavaScript subset. It evaluates the AST produced by
 * {@link Parser} against a chain of {@link Environment}s, using lightweight unchecked exceptions to
 * implement {@code return}/{@code break}/{@code continue}. Objects, arrays, closures, first-class
 * functions, {@code this}, and the common operators are supported; generators, prototypes,
 * async/await, and exceptions ({@code try/catch}) are not.
 */
public final class Interpreter {

    /** Non-local exit used to implement {@code return}. */
    public static final class ReturnSignal extends RuntimeException {
        private final transient Object value;

        public ReturnSignal(Object value) {
            super(null, null, false, false);
            this.value = value;
        }

        public Object value() {
            return value;
        }
    }

    static final class BreakSignal extends RuntimeException {
        BreakSignal() {
            super(null, null, false, false);
        }
    }

    static final class ContinueSignal extends RuntimeException {
        ContinueSignal() {
            super(null, null, false, false);
        }
    }

    private final Environment global = new Environment(null);
    private final List<String> output = new ArrayList<>();

    // Bound runaway scripts: an infinite loop or unbounded recursion must fail fast, not hang the
    // rendering thread or blow the Java stack.
    private static final long MAX_STEPS = 10_000_000L;
    private static final int MAX_CALL_DEPTH = 1000;
    private long steps;
    private int callDepth;

    public Interpreter() {
        Builtins.installGlobals(this, global);
    }

    private void tick() {
        if (++steps > MAX_STEPS) {
            throw new JsException("RangeError: script exceeded its execution budget");
        }
    }

    public Environment global() {
        return global;
    }

    public List<String> consoleOutput() {
        return output;
    }

    public void print(String line) {
        output.add(line);
    }

    public Object run(String source) {
        return run(new Parser(source).parseProgram());
    }

    public Object run(Program program) {
        try {
            executeStatements(program.body(), global);
        } catch (StackOverflowError e) {
            throw new JsException("RangeError: Maximum call stack size exceeded");
        }
        return Undefined.VALUE;
    }

    public void executeStatements(List<Stmt> statements, Environment env) {
        for (Stmt s : statements) {
            if (s instanceof FunctionDecl fd) {
                env.define(fd.name(), new UserFunction(fd.name(), fd.params(), fd.body(), env));
            }
        }
        for (Stmt s : statements) {
            execute(s, env);
        }
    }

    // ---- Statements -----------------------------------------------------------------------------

    void execute(Stmt stmt, Environment env) {
        tick();
        if (stmt instanceof ExprStmt e) {
            evaluate(e.expression(), env);
        } else if (stmt instanceof VarDecl v) {
            for (Declarator d : v.declarations()) {
                env.define(d.name(), d.init() == null ? Undefined.VALUE : evaluate(d.init(), env));
            }
        } else if (stmt instanceof Block b) {
            executeStatements(b.statements(), new Environment(env));
        } else if (stmt instanceof Ast.If i) {
            if (JsValues.truthy(evaluate(i.test(), env))) {
                execute(i.consequent(), env);
            } else if (i.alternate() != null) {
                execute(i.alternate(), env);
            }
        } else if (stmt instanceof Ast.While w) {
            while (JsValues.truthy(evaluate(w.test(), env))) {
                try {
                    execute(w.body(), env);
                } catch (ContinueSignal c) {
                    // next iteration
                } catch (BreakSignal b) {
                    break;
                }
            }
        } else if (stmt instanceof Ast.For f) {
            executeFor(f, env);
        } else if (stmt instanceof ForEach fe) {
            executeForEach(fe, env);
        } else if (stmt instanceof Ast.Return r) {
            throw new ReturnSignal(r.argument() == null ? Undefined.VALUE : evaluate(r.argument(), env));
        } else if (stmt instanceof FunctionDecl fd) {
            env.define(fd.name(), new UserFunction(fd.name(), fd.params(), fd.body(), env));
        } else if (stmt instanceof Ast.Break) {
            throw new BreakSignal();
        } else if (stmt instanceof Ast.Continue) {
            throw new ContinueSignal();
        } else {
            throw new JsException("Unsupported statement: " + stmt);
        }
    }

    private void executeFor(Ast.For f, Environment env) {
        Environment forEnv = new Environment(env);
        if (f.init() != null) {
            execute(f.init(), forEnv);
        }
        while (f.test() == null || JsValues.truthy(evaluate(f.test(), forEnv))) {
            try {
                execute(f.body(), forEnv);
            } catch (ContinueSignal c) {
                // fall through to update
            } catch (BreakSignal b) {
                break;
            }
            if (f.update() != null) {
                evaluate(f.update(), forEnv);
            }
        }
    }

    private void executeForEach(ForEach fe, Environment env) {
        Object obj = evaluate(fe.object(), env);
        List<Object> sequence = new ArrayList<>();
        if (fe.ofLoop()) {
            if (obj instanceof JsArray a) {
                sequence.addAll(a.items());
            } else if (obj instanceof String s) {
                for (int i = 0; i < s.length(); i++) {
                    sequence.add(String.valueOf(s.charAt(i)));
                }
            } else {
                throw new JsException("TypeError: value is not iterable");
            }
        } else if (obj instanceof JsArray a) {
            for (int i = 0; i < a.items().size(); i++) {
                sequence.add(String.valueOf(i));
            }
        } else if (obj instanceof JsObject o) {
            sequence.addAll(o.properties().keySet());
        }
        for (Object item : sequence) {
            Environment iter = new Environment(env);
            iter.define(fe.name(), item);
            try {
                execute(fe.body(), iter);
            } catch (ContinueSignal c) {
                // next iteration
            } catch (BreakSignal b) {
                break;
            }
        }
    }

    // ---- Expressions ----------------------------------------------------------------------------

    public Object evaluate(Expr expr, Environment env) {
        tick();
        if (expr instanceof NumberLit n) {
            return n.value();
        }
        if (expr instanceof StringLit s) {
            return s.value();
        }
        if (expr instanceof BoolLit b) {
            return b.value();
        }
        if (expr instanceof Ast.NullLit) {
            return null;
        }
        if (expr instanceof UndefinedLit) {
            return Undefined.VALUE;
        }
        if (expr instanceof Identifier id) {
            return env.get(id.name());
        }
        if (expr instanceof ThisExpr) {
            return env.has("this") ? env.get("this") : Undefined.VALUE;
        }
        if (expr instanceof ArrayLit a) {
            List<Object> items = new ArrayList<>();
            for (Expr e : a.elements()) {
                items.add(evaluate(e, env));
            }
            return new JsArray(items);
        }
        if (expr instanceof ObjectLit o) {
            JsObject obj = new JsObject();
            for (Property p : o.properties()) {
                obj.set(p.key(), evaluate(p.value(), env));
            }
            return obj;
        }
        if (expr instanceof Unary u) {
            return evalUnary(u, env);
        }
        if (expr instanceof Update u) {
            return evalUpdate(u, env);
        }
        if (expr instanceof Binary b) {
            return applyBinary(b.op(), evaluate(b.left(), env), evaluate(b.right(), env));
        }
        if (expr instanceof Logical l) {
            return evalLogical(l, env);
        }
        if (expr instanceof Assign a) {
            return evalAssign(a, env);
        }
        if (expr instanceof Conditional c) {
            return JsValues.truthy(evaluate(c.test(), env))
                    ? evaluate(c.consequent(), env)
                    : evaluate(c.alternate(), env);
        }
        if (expr instanceof Member m) {
            return getMember(evaluate(m.object(), env), memberName(m, env));
        }
        if (expr instanceof Call c) {
            return evalCall(c, env);
        }
        if (expr instanceof NewExpr n) {
            return evalNew(n, env);
        }
        if (expr instanceof FunctionExpr f) {
            return makeFunction(f, env);
        }
        throw new JsException("Unsupported expression: " + expr);
    }

    private Object makeFunction(FunctionExpr f, Environment env) {
        if (f.name() != null) {
            Environment named = new Environment(env);
            UserFunction fn = new UserFunction(f.name(), f.params(), f.body(), named);
            named.define(f.name(), fn);
            return fn;
        }
        return new UserFunction(null, f.params(), f.body(), env);
    }

    private String memberName(Member m, Environment env) {
        return m.computed()
                ? JsValues.stringify(evaluate(m.property(), env))
                : ((Identifier) m.property()).name();
    }

    private Object evalUnary(Unary u, Environment env) {
        if (u.op().equals("typeof") && u.operand() instanceof Identifier id && !env.has(id.name())) {
            return "undefined";
        }
        Object v = evaluate(u.operand(), env);
        return switch (u.op()) {
            case "!" -> !JsValues.truthy(v);
            case "-" -> -JsValues.toNumber(v);
            case "+" -> JsValues.toNumber(v);
            case "typeof" -> JsValues.typeOf(v);
            default -> throw new JsException("Unknown unary operator " + u.op());
        };
    }

    private Object evalUpdate(Update u, Environment env) {
        double current = JsValues.toNumber(evaluate(u.target(), env));
        double updated = u.op().equals("++") ? current + 1 : current - 1;
        assignTo(u.target(), updated, env);
        return u.prefix() ? updated : current;
    }

    private Object evalLogical(Logical l, Environment env) {
        Object left = evaluate(l.left(), env);
        if (l.op().equals("&&")) {
            return JsValues.truthy(left) ? evaluate(l.right(), env) : left;
        }
        return JsValues.truthy(left) ? left : evaluate(l.right(), env);
    }

    private Object evalAssign(Assign a, Environment env) {
        if (a.op().equals("=")) {
            Object value = evaluate(a.value(), env);
            assignTo(a.target(), value, env);
            return value;
        }
        Object current = evaluate(a.target(), env);
        Object operand = evaluate(a.value(), env);
        Object result = applyBinary(a.op().substring(0, a.op().length() - 1), current, operand);
        assignTo(a.target(), result, env);
        return result;
    }

    private void assignTo(Expr target, Object value, Environment env) {
        if (target instanceof Identifier id) {
            env.assign(id.name(), value);
        } else if (target instanceof Member m) {
            setMember(evaluate(m.object(), env), memberName(m, env), value);
        } else {
            throw new JsException("Invalid assignment target");
        }
    }

    private Object evalCall(Call c, Environment env) {
        Object thisValue = Undefined.VALUE;
        Object fn;
        if (c.callee() instanceof Member m) {
            Object obj = evaluate(m.object(), env);
            thisValue = obj;
            fn = getMember(obj, memberName(m, env));
        } else {
            fn = evaluate(c.callee(), env);
        }
        List<Object> args = new ArrayList<>();
        for (Expr e : c.arguments()) {
            args.add(evaluate(e, env));
        }
        return callFunction(fn, thisValue, args);
    }

    public Object callFunction(Object fn, Object thisValue, List<Object> args) {
        if (fn instanceof JsCallable callable) {
            if (++callDepth > MAX_CALL_DEPTH) {
                callDepth--;
                throw new JsException("RangeError: Maximum call stack size exceeded");
            }
            try {
                return callable.call(this, thisValue, args);
            } finally {
                callDepth--;
            }
        }
        throw new JsException("TypeError: " + JsValues.stringify(fn) + " is not a function");
    }

    private Object evalNew(NewExpr n, Environment env) {
        Object calleeVal = evaluate(n.callee(), env);
        if (!(calleeVal instanceof JsCallable callable)) {
            throw new JsException("TypeError: value is not a constructor");
        }
        List<Object> args = new ArrayList<>();
        for (Expr e : n.arguments()) {
            args.add(evaluate(e, env));
        }
        JsObject instance = new JsObject();
        Object result = callable.call(this, instance, args);
        return result instanceof JsObject ? result : instance;
    }

    // ---- Member access --------------------------------------------------------------------------

    public Object getMember(Object obj, String name) {
        if (obj == null || obj == Undefined.VALUE) {
            throw new JsException("TypeError: Cannot read properties of " + JsValues.stringify(obj)
                    + " (reading '" + name + "')");
        }
        if (obj instanceof JsArray arr) {
            Object method = Builtins.arrayMember(this, arr, name);
            return method != null ? method : arr.get(name);
        }
        if (obj instanceof JsObject o) {
            return o.get(name);
        }
        if (obj instanceof String s) {
            Object method = Builtins.stringMember(this, s, name);
            return method != null ? method : Undefined.VALUE;
        }
        if (obj instanceof Double d) {
            Object method = Builtins.numberMember(this, d, name);
            return method != null ? method : Undefined.VALUE;
        }
        return Undefined.VALUE;
    }

    public void setMember(Object obj, String name, Object value) {
        if (obj instanceof JsObject o) {
            o.set(name, value);
        }
    }

    // ---- Operators ------------------------------------------------------------------------------

    private Object applyBinary(String op, Object l, Object r) {
        switch (op) {
            case "+":
                if (l instanceof String || r instanceof String) {
                    return JsValues.stringify(l) + JsValues.stringify(r);
                }
                return JsValues.toNumber(l) + JsValues.toNumber(r);
            case "-":
                return JsValues.toNumber(l) - JsValues.toNumber(r);
            case "*":
                return JsValues.toNumber(l) * JsValues.toNumber(r);
            case "/":
                return JsValues.toNumber(l) / JsValues.toNumber(r);
            case "%":
                return JsValues.toNumber(l) % JsValues.toNumber(r);
            case "<":
            case ">":
            case "<=":
            case ">=":
                return compare(op, l, r);
            case "==":
                return JsValues.looseEquals(l, r);
            case "!=":
                return !JsValues.looseEquals(l, r);
            case "===":
                return JsValues.strictEquals(l, r);
            case "!==":
                return !JsValues.strictEquals(l, r);
            default:
                throw new JsException("Unknown operator " + op);
        }
    }

    private boolean compare(String op, Object l, Object r) {
        if (l instanceof String ls && r instanceof String rs) {
            int c = ls.compareTo(rs);
            return switch (op) {
                case "<" -> c < 0;
                case ">" -> c > 0;
                case "<=" -> c <= 0;
                default -> c >= 0;
            };
        }
        double a = JsValues.toNumber(l);
        double b = JsValues.toNumber(r);
        if (Double.isNaN(a) || Double.isNaN(b)) {
            return false;
        }
        return switch (op) {
            case "<" -> a < b;
            case ">" -> a > b;
            case "<=" -> a <= b;
            default -> a >= b;
        };
    }
}
