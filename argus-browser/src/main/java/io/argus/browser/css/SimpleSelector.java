package io.argus.browser.css;

import io.argus.browser.dom.Element;
import java.util.List;

/**
 * A compound simple selector: an optional type/universal, an optional id, and zero or more classes
 * (e.g. {@code div.note#main}). Matches a single element without considering its ancestors.
 */
public final class SimpleSelector {

    private final String tag; // null or "*" means any type
    private final String id;  // null means no id constraint
    private final List<String> classes;

    public SimpleSelector(String tag, String id, List<String> classes) {
        this.tag = tag;
        this.id = id;
        this.classes = List.copyOf(classes);
    }

    public boolean matches(Element e) {
        if (tag != null && !tag.equals("*") && !e.tagName().equals(tag)) {
            return false;
        }
        if (id != null && !id.equals(e.getAttribute("id"))) {
            return false;
        }
        for (String c : classes) {
            if (!e.classList().contains(c)) {
                return false;
            }
        }
        return true;
    }

    public Specificity specificity() {
        int idCount = id != null ? 1 : 0;
        int typeCount = (tag != null && !tag.equals("*")) ? 1 : 0;
        return new Specificity(idCount, classes.size(), typeCount);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (tag != null) {
            sb.append(tag);
        }
        if (id != null) {
            sb.append('#').append(id);
        }
        for (String c : classes) {
            sb.append('.').append(c);
        }
        return sb.toString();
    }
}
