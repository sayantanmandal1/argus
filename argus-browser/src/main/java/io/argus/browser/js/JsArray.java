package io.argus.browser.js;

import java.util.ArrayList;
import java.util.List;

/** A JavaScript array: an ordered list with live {@code length} and numeric-index property access. */
public final class JsArray extends JsObject {

    private final List<Object> items;

    public JsArray() {
        this.items = new ArrayList<>();
    }

    public JsArray(List<Object> initial) {
        this.items = new ArrayList<>(initial);
    }

    public List<Object> items() {
        return items;
    }

    @Override
    public Object get(String name) {
        if (name.equals("length")) {
            return (double) items.size();
        }
        Integer index = asIndex(name);
        if (index != null) {
            return index >= 0 && index < items.size() ? items.get(index) : Undefined.VALUE;
        }
        return super.get(name);
    }

    @Override
    public void set(String name, Object value) {
        if (name.equals("length")) {
            int newSize = (int) JsValues.toNumber(value);
            while (items.size() > newSize) {
                items.remove(items.size() - 1);
            }
            while (items.size() < newSize) {
                items.add(Undefined.VALUE);
            }
            return;
        }
        Integer index = asIndex(name);
        if (index != null && index >= 0) {
            while (items.size() <= index) {
                items.add(Undefined.VALUE);
            }
            items.set(index, value);
            return;
        }
        super.set(name, value);
    }

    private static Integer asIndex(String name) {
        if (name.isEmpty()) {
            return null;
        }
        for (int i = 0; i < name.length(); i++) {
            if (!Character.isDigit(name.charAt(i))) {
                return null;
            }
        }
        try {
            return Integer.parseInt(name);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
