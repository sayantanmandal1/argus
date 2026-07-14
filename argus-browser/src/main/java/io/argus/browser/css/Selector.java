package io.argus.browser.css;

import io.argus.browser.dom.Element;
import io.argus.browser.dom.Node;
import java.util.List;

/**
 * A full selector: a sequence of {@link SimpleSelector}s joined by descendant combinators. The last
 * entry must match the candidate element; each earlier entry must match some ancestor, in order
 * (e.g. {@code nav ul li a} matches an {@code <a>} anywhere under a {@code <li>} under a {@code <ul>}
 * under a {@code <nav>}).
 */
public final class Selector {

    private final List<SimpleSelector> sequence;

    public Selector(List<SimpleSelector> sequence) {
        this.sequence = List.copyOf(sequence);
    }

    public boolean matches(Element target) {
        int i = sequence.size() - 1;
        if (!sequence.get(i).matches(target)) {
            return false;
        }
        i--;
        Node current = target.parent();
        while (i >= 0 && current != null) {
            if (current instanceof Element e && sequence.get(i).matches(e)) {
                i--;
            }
            current = current.parent();
        }
        return i < 0;
    }

    public Specificity specificity() {
        Specificity total = new Specificity(0, 0, 0);
        for (SimpleSelector s : sequence) {
            total = total.plus(s.specificity());
        }
        return total;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sequence.size(); i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(sequence.get(i));
        }
        return sb.toString();
    }
}
