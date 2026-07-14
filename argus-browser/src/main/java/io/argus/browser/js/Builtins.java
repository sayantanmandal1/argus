package io.argus.browser.js;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The standard library surface the interpreter exposes: global functions ({@code parseInt},
 * {@code isNaN}, {@code String}, ...), the {@code console} and {@code Math} objects, and the method
 * tables for primitive strings, numbers, and arrays. A pragmatic subset, not the full ECMAScript
 * library.
 */
final class Builtins {

    private static final Pattern FLOAT = Pattern.compile("^[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?");

    private Builtins() {
    }

    static void installGlobals(Interpreter interp, Environment g) {
        g.define("undefined", Undefined.VALUE);
        g.define("NaN", Double.NaN);
        g.define("Infinity", Double.POSITIVE_INFINITY);

        JsObject console = new JsObject();
        JsCallable log = (in, t, a) -> {
            in.print(a.stream().map(JsValues::stringify).collect(Collectors.joining(" ")));
            return Undefined.VALUE;
        };
        for (String name : new String[] {"log", "info", "warn", "error", "debug"}) {
            console.set(name, log);
        }
        g.define("console", console);

        g.define("Math", math());

        g.define("parseInt", (JsCallable) (in, t, a) -> parseInt(str(a, 0), a.size() > 1 ? (int) num(a, 1) : 10));
        g.define("parseFloat", (JsCallable) (in, t, a) -> parseFloat(str(a, 0)));
        g.define("isNaN", (JsCallable) (in, t, a) -> Double.isNaN(JsValues.toNumber(arg(a, 0))));
        g.define("isFinite", (JsCallable) (in, t, a) -> {
            double d = JsValues.toNumber(arg(a, 0));
            return !Double.isNaN(d) && !Double.isInfinite(d);
        });
        g.define("String", (JsCallable) (in, t, a) -> a.isEmpty() ? "" : JsValues.stringify(a.get(0)));
        g.define("Number", (JsCallable) (in, t, a) -> a.isEmpty() ? 0.0 : JsValues.toNumber(a.get(0)));
        g.define("Boolean", (JsCallable) (in, t, a) -> !a.isEmpty() && JsValues.truthy(a.get(0)));
    }

    private static JsObject math() {
        JsObject math = new JsObject();
        math.set("PI", Math.PI);
        math.set("E", Math.E);
        math.set("floor", (JsCallable) (in, t, a) -> Math.floor(num(a, 0)));
        math.set("ceil", (JsCallable) (in, t, a) -> Math.ceil(num(a, 0)));
        math.set("round", (JsCallable) (in, t, a) -> Math.floor(num(a, 0) + 0.5));
        math.set("trunc", (JsCallable) (in, t, a) -> (double) (long) num(a, 0));
        math.set("abs", (JsCallable) (in, t, a) -> Math.abs(num(a, 0)));
        math.set("sign", (JsCallable) (in, t, a) -> Math.signum(num(a, 0)));
        math.set("sqrt", (JsCallable) (in, t, a) -> Math.sqrt(num(a, 0)));
        math.set("cbrt", (JsCallable) (in, t, a) -> Math.cbrt(num(a, 0)));
        math.set("pow", (JsCallable) (in, t, a) -> Math.pow(num(a, 0), num(a, 1)));
        math.set("exp", (JsCallable) (in, t, a) -> Math.exp(num(a, 0)));
        math.set("log", (JsCallable) (in, t, a) -> Math.log(num(a, 0)));
        math.set("sin", (JsCallable) (in, t, a) -> Math.sin(num(a, 0)));
        math.set("cos", (JsCallable) (in, t, a) -> Math.cos(num(a, 0)));
        math.set("tan", (JsCallable) (in, t, a) -> Math.tan(num(a, 0)));
        math.set("random", (JsCallable) (in, t, a) -> Math.random());
        math.set("max", (JsCallable) (in, t, a) -> {
            double m = Double.NEGATIVE_INFINITY;
            for (Object o : a) {
                m = Math.max(m, JsValues.toNumber(o));
            }
            return m;
        });
        math.set("min", (JsCallable) (in, t, a) -> {
            double m = Double.POSITIVE_INFINITY;
            for (Object o : a) {
                m = Math.min(m, JsValues.toNumber(o));
            }
            return m;
        });
        return math;
    }

    // ---- String methods -------------------------------------------------------------------------

    static Object stringMember(Interpreter interp, String s, String name) {
        switch (name) {
            case "length":
                return (double) s.length();
            case "charAt":
                return (JsCallable) (in, t, a) -> {
                    int i = (int) num(a, 0);
                    return i >= 0 && i < s.length() ? String.valueOf(s.charAt(i)) : "";
                };
            case "charCodeAt":
                return (JsCallable) (in, t, a) -> {
                    int i = (int) num(a, 0);
                    return i >= 0 && i < s.length() ? (double) s.charAt(i) : Double.NaN;
                };
            case "toUpperCase":
                return (JsCallable) (in, t, a) -> s.toUpperCase();
            case "toLowerCase":
                return (JsCallable) (in, t, a) -> s.toLowerCase();
            case "trim":
                return (JsCallable) (in, t, a) -> s.trim();
            case "indexOf":
                return (JsCallable) (in, t, a) -> (double) s.indexOf(str(a, 0));
            case "lastIndexOf":
                return (JsCallable) (in, t, a) -> (double) s.lastIndexOf(str(a, 0));
            case "includes":
                return (JsCallable) (in, t, a) -> s.contains(str(a, 0));
            case "startsWith":
                return (JsCallable) (in, t, a) -> s.startsWith(str(a, 0));
            case "endsWith":
                return (JsCallable) (in, t, a) -> s.endsWith(str(a, 0));
            case "slice":
                return (JsCallable) (in, t, a) -> {
                    int n = s.length();
                    int start = normIndex(arg(a, 0), n, 0);
                    int end = a.size() > 1 ? normIndex(arg(a, 1), n, n) : n;
                    return start < end ? s.substring(start, end) : "";
                };
            case "substring":
                return (JsCallable) (in, t, a) -> {
                    int n = s.length();
                    int start = clamp((int) num(a, 0), 0, n);
                    int end = a.size() > 1 ? clamp((int) num(a, 1), 0, n) : n;
                    return start <= end ? s.substring(start, end) : s.substring(end, start);
                };
            case "split":
                return (JsCallable) (in, t, a) -> split(s, a);
            case "replace":
                return (JsCallable) (in, t, a) -> {
                    String find = str(a, 0);
                    String rep = str(a, 1);
                    int idx = s.indexOf(find);
                    return idx < 0 ? s : s.substring(0, idx) + rep + s.substring(idx + find.length());
                };
            case "repeat":
                return (JsCallable) (in, t, a) -> s.repeat(Math.max(0, (int) num(a, 0)));
            case "concat":
                return (JsCallable) (in, t, a) -> {
                    StringBuilder sb = new StringBuilder(s);
                    for (Object o : a) {
                        sb.append(JsValues.stringify(o));
                    }
                    return sb.toString();
                };
            case "toString":
                return (JsCallable) (in, t, a) -> s;
            default:
                return null;
        }
    }

    private static Object split(String s, List<Object> a) {
        if (a.isEmpty() || arg(a, 0) == Undefined.VALUE) {
            return new JsArray(List.of(s));
        }
        String sep = str(a, 0);
        List<Object> parts = new ArrayList<>();
        if (sep.isEmpty()) {
            for (int i = 0; i < s.length(); i++) {
                parts.add(String.valueOf(s.charAt(i)));
            }
        } else {
            parts.addAll(Arrays.asList(s.split(Pattern.quote(sep), -1)));
        }
        return new JsArray(parts);
    }

    // ---- Number methods -------------------------------------------------------------------------

    static Object numberMember(Interpreter interp, Double d, String name) {
        switch (name) {
            case "toFixed":
                return (JsCallable) (in, t, a) -> String.format("%." + (int) num(a, 0) + "f", d);
            case "toString":
                return (JsCallable) (in, t, a) -> JsValues.numberToString(d);
            default:
                return null;
        }
    }

    // ---- Array methods --------------------------------------------------------------------------

    static Object arrayMember(Interpreter interp, JsArray arr, String name) {
        List<Object> items = arr.items();
        switch (name) {
            case "push":
                return (JsCallable) (in, t, a) -> {
                    items.addAll(a);
                    return (double) items.size();
                };
            case "pop":
                return (JsCallable) (in, t, a) -> items.isEmpty() ? Undefined.VALUE : items.remove(items.size() - 1);
            case "shift":
                return (JsCallable) (in, t, a) -> items.isEmpty() ? Undefined.VALUE : items.remove(0);
            case "unshift":
                return (JsCallable) (in, t, a) -> {
                    items.addAll(0, a);
                    return (double) items.size();
                };
            case "join":
                return (JsCallable) (in, t, a) -> join(items, a.isEmpty() ? "," : str(a, 0));
            case "indexOf":
                return (JsCallable) (in, t, a) -> {
                    Object target = arg(a, 0);
                    for (int i = 0; i < items.size(); i++) {
                        if (JsValues.strictEquals(items.get(i), target)) {
                            return (double) i;
                        }
                    }
                    return -1.0;
                };
            case "includes":
                return (JsCallable) (in, t, a) -> {
                    Object target = arg(a, 0);
                    for (Object o : items) {
                        if (JsValues.strictEquals(o, target)) {
                            return true;
                        }
                    }
                    return false;
                };
            case "slice":
                return (JsCallable) (in, t, a) -> {
                    int n = items.size();
                    int start = normIndex(arg(a, 0), n, 0);
                    int end = a.size() > 1 ? normIndex(arg(a, 1), n, n) : n;
                    List<Object> out = new ArrayList<>();
                    for (int i = start; i < end; i++) {
                        out.add(items.get(i));
                    }
                    return new JsArray(out);
                };
            case "concat":
                return (JsCallable) (in, t, a) -> {
                    List<Object> out = new ArrayList<>(items);
                    for (Object o : a) {
                        if (o instanceof JsArray other) {
                            out.addAll(other.items());
                        } else {
                            out.add(o);
                        }
                    }
                    return new JsArray(out);
                };
            case "reverse":
                return (JsCallable) (in, t, a) -> {
                    Collections.reverse(items);
                    return arr;
                };
            case "map":
                return (JsCallable) (in, t, a) -> {
                    Object fn = arg(a, 0);
                    List<Object> out = new ArrayList<>();
                    for (int i = 0; i < items.size(); i++) {
                        out.add(in.callFunction(fn, Undefined.VALUE, Arrays.asList(items.get(i), (double) i, arr)));
                    }
                    return new JsArray(out);
                };
            case "filter":
                return (JsCallable) (in, t, a) -> {
                    Object fn = arg(a, 0);
                    List<Object> out = new ArrayList<>();
                    for (int i = 0; i < items.size(); i++) {
                        Object item = items.get(i);
                        if (JsValues.truthy(in.callFunction(fn, Undefined.VALUE, Arrays.asList(item, (double) i, arr)))) {
                            out.add(item);
                        }
                    }
                    return new JsArray(out);
                };
            case "forEach":
                return (JsCallable) (in, t, a) -> {
                    Object fn = arg(a, 0);
                    for (int i = 0; i < items.size(); i++) {
                        in.callFunction(fn, Undefined.VALUE, Arrays.asList(items.get(i), (double) i, arr));
                    }
                    return Undefined.VALUE;
                };
            default:
                return null;
        }
    }

    private static String join(List<Object> items, String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append(sep);
            }
            Object item = items.get(i);
            sb.append(item == null || item == Undefined.VALUE ? "" : JsValues.stringify(item));
        }
        return sb.toString();
    }

    // ---- Helpers --------------------------------------------------------------------------------

    private static Object parseInt(String s, int radix) {
        String v = s.trim();
        if (v.isEmpty()) {
            return Double.NaN;
        }
        int i = 0;
        int sign = 1;
        if (v.charAt(i) == '+' || v.charAt(i) == '-') {
            sign = v.charAt(i) == '-' ? -1 : 1;
            i++;
        }
        if (radix == 0) {
            radix = 10;
        }
        if (radix == 16 && i + 1 < v.length() && v.charAt(i) == '0'
                && (v.charAt(i + 1) == 'x' || v.charAt(i + 1) == 'X')) {
            i += 2;
        }
        long value = 0;
        boolean any = false;
        for (; i < v.length(); i++) {
            int digit = Character.digit(v.charAt(i), radix);
            if (digit < 0) {
                break;
            }
            value = value * radix + digit;
            any = true;
        }
        return any ? (double) (sign * value) : Double.NaN;
    }

    private static Object parseFloat(String s) {
        Matcher m = FLOAT.matcher(s.trim());
        return m.find() ? Double.parseDouble(m.group()) : Double.NaN;
    }

    private static Object arg(List<Object> args, int i) {
        return i < args.size() ? args.get(i) : Undefined.VALUE;
    }

    private static double num(List<Object> args, int i) {
        return JsValues.toNumber(arg(args, i));
    }

    private static String str(List<Object> args, int i) {
        return JsValues.stringify(arg(args, i));
    }

    private static int normIndex(Object v, int length, int def) {
        if (v == Undefined.VALUE) {
            return def;
        }
        int idx = (int) JsValues.toNumber(v);
        if (idx < 0) {
            idx += length;
        }
        return clamp(idx, 0, length);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
