package io.argus.browser.js;

import java.util.HashMap;
import java.util.Map;

/** A lexical scope: a set of bindings plus a link to the enclosing scope, forming the scope chain. */
public final class Environment {

    private final Environment parent;
    private final Map<String, Object> values = new HashMap<>();

    public Environment(Environment parent) {
        this.parent = parent;
    }

    public void define(String name, Object value) {
        values.put(name, value);
    }

    public boolean has(String name) {
        return values.containsKey(name) || (parent != null && parent.has(name));
    }

    public Object get(String name) {
        if (values.containsKey(name)) {
            return values.get(name);
        }
        if (parent != null) {
            return parent.get(name);
        }
        throw new JsException("ReferenceError: " + name + " is not defined");
    }

    /** Assigns to the nearest existing binding, or creates a global binding if none exists. */
    public void assign(String name, Object value) {
        for (Environment env = this; env != null; env = env.parent) {
            if (env.values.containsKey(name)) {
                env.values.put(name, value);
                return;
            }
        }
        Environment root = this;
        while (root.parent != null) {
            root = root.parent;
        }
        root.values.put(name, value);
    }
}
