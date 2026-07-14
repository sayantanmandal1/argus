package io.argus.browser.js;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A JavaScript object: an ordered string-keyed property bag. DOM wrappers subclass this and override
 * {@link #get}/{@link #set} to project live element state as properties.
 */
public class JsObject {

    protected final Map<String, Object> properties = new LinkedHashMap<>();

    public Object get(String name) {
        return properties.getOrDefault(name, Undefined.VALUE);
    }

    public void set(String name, Object value) {
        properties.put(name, value);
    }

    public boolean has(String name) {
        return properties.containsKey(name);
    }

    public Map<String, Object> properties() {
        return properties;
    }
}
