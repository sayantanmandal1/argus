package io.argus.browser.css;

import java.util.List;

/** A parsed stylesheet: an ordered list of {@link Rule}s. */
public final class Stylesheet {

    private final List<Rule> rules;

    public Stylesheet(List<Rule> rules) {
        this.rules = List.copyOf(rules);
    }

    public List<Rule> rules() {
        return rules;
    }
}
