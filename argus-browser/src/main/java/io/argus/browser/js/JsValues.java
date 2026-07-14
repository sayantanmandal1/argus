package io.argus.browser.js;

/** JavaScript value coercions shared by the interpreter: truthiness, ToNumber, ToString, equality. */
public final class JsValues {

    private JsValues() {
    }

    public static boolean truthy(Object v) {
        if (v == null || v == Undefined.VALUE) {
            return false;
        }
        if (v instanceof Boolean b) {
            return b;
        }
        if (v instanceof Double d) {
            return d != 0 && !Double.isNaN(d);
        }
        if (v instanceof String s) {
            return !s.isEmpty();
        }
        return true;
    }

    public static double toNumber(Object v) {
        if (v == null) {
            return 0;
        }
        if (v == Undefined.VALUE) {
            return Double.NaN;
        }
        if (v instanceof Boolean b) {
            return b ? 1 : 0;
        }
        if (v instanceof Double d) {
            return d;
        }
        if (v instanceof String s) {
            String t = s.trim();
            if (t.isEmpty()) {
                return 0;
            }
            try {
                return Double.parseDouble(t);
            } catch (NumberFormatException e) {
                return Double.NaN;
            }
        }
        return Double.NaN;
    }

    public static String stringify(Object v) {
        if (v == null) {
            return "null";
        }
        if (v == Undefined.VALUE) {
            return "undefined";
        }
        if (v instanceof Boolean b) {
            return b.toString();
        }
        if (v instanceof Double d) {
            return numberToString(d);
        }
        if (v instanceof String s) {
            return s;
        }
        if (v instanceof JsArray a) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < a.items().size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                Object item = a.items().get(i);
                sb.append(item == null || item == Undefined.VALUE ? "" : stringify(item));
            }
            return sb.toString();
        }
        if (v instanceof JsCallable) {
            return "function";
        }
        if (v instanceof JsObject) {
            return "[object Object]";
        }
        return String.valueOf(v);
    }

    public static String numberToString(double d) {
        if (Double.isNaN(d)) {
            return "NaN";
        }
        if (Double.isInfinite(d)) {
            return d > 0 ? "Infinity" : "-Infinity";
        }
        if (d == Math.rint(d) && Math.abs(d) < 1e15) {
            return Long.toString((long) d);
        }
        return Double.toString(d);
    }

    public static String typeOf(Object v) {
        if (v == Undefined.VALUE) {
            return "undefined";
        }
        if (v == null) {
            return "object";
        }
        if (v instanceof Boolean) {
            return "boolean";
        }
        if (v instanceof Double) {
            return "number";
        }
        if (v instanceof String) {
            return "string";
        }
        if (v instanceof JsCallable) {
            return "function";
        }
        return "object";
    }

    public static boolean strictEquals(Object a, Object b) {
        if (a == Undefined.VALUE || b == Undefined.VALUE || a == null || b == null) {
            return a == b;
        }
        if (a instanceof Double x && b instanceof Double y) {
            return x.doubleValue() == y.doubleValue();
        }
        if (a instanceof String x && b instanceof String y) {
            return x.equals(y);
        }
        if (a instanceof Boolean x && b instanceof Boolean y) {
            return x.equals(y);
        }
        return a == b;
    }

    public static boolean looseEquals(Object a, Object b) {
        boolean aNullish = a == null || a == Undefined.VALUE;
        boolean bNullish = b == null || b == Undefined.VALUE;
        if (aNullish || bNullish) {
            return aNullish && bNullish;
        }
        if (a.getClass() == b.getClass()) {
            return strictEquals(a, b);
        }
        if (a instanceof Boolean ab) {
            return looseEquals(ab ? 1.0 : 0.0, b);
        }
        if (b instanceof Boolean bb) {
            return looseEquals(a, bb ? 1.0 : 0.0);
        }
        if ((a instanceof Double || a instanceof String) && (b instanceof Double || b instanceof String)) {
            return toNumber(a) == toNumber(b);
        }
        return false;
    }
}
