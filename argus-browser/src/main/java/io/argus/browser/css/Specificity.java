package io.argus.browser.css;

/**
 * CSS selector specificity as the standard (id, class, type) triple, compared left-to-right. Higher
 * specificity wins in the cascade when two rules set the same property.
 */
public record Specificity(int ids, int classes, int types) implements Comparable<Specificity> {

    public Specificity plus(Specificity other) {
        return new Specificity(ids + other.ids, classes + other.classes, types + other.types);
    }

    @Override
    public int compareTo(Specificity o) {
        if (ids != o.ids) {
            return Integer.compare(ids, o.ids);
        }
        if (classes != o.classes) {
            return Integer.compare(classes, o.classes);
        }
        return Integer.compare(types, o.types);
    }
}
