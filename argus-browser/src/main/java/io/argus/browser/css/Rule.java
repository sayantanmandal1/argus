package io.argus.browser.css;

import java.util.List;

/** A CSS rule: one or more selectors (a selector list) sharing a block of declarations. */
public final class Rule {

    private final List<Selector> selectors;
    private final List<Declaration> declarations;

    public Rule(List<Selector> selectors, List<Declaration> declarations) {
        this.selectors = List.copyOf(selectors);
        this.declarations = List.copyOf(declarations);
    }

    public List<Selector> selectors() {
        return selectors;
    }

    public List<Declaration> declarations() {
        return declarations;
    }
}
