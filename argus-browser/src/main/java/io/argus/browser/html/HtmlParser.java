package io.argus.browser.html;

import io.argus.browser.dom.Comment;
import io.argus.browser.dom.Document;
import io.argus.browser.dom.Element;
import io.argus.browser.dom.Node;
import io.argus.browser.dom.Text;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds a {@link Document} tree from a token stream using a stack of open elements. Void elements
 * (e.g. {@code <br>}, {@code <img>}) never receive children, and a stray end tag that matches no open
 * element is ignored — the same forgiving behavior real browsers rely on.
 */
public final class HtmlParser {

    private static final Set<String> VOID_ELEMENTS = Set.of(
            "area", "base", "br", "col", "embed", "hr", "img", "input",
            "link", "meta", "param", "source", "track", "wbr");

    public Document parse(String html) {
        List<Token> tokens = new HtmlTokenizer(html).tokenize();
        Document document = new Document();
        Deque<Node> stack = new ArrayDeque<>();
        stack.push(document);

        for (Token token : tokens) {
            if (token instanceof Token.StartTag start) {
                Element element = new Element(start.name());
                for (Map.Entry<String, String> attr : start.attributes().entrySet()) {
                    element.setAttribute(attr.getKey(), attr.getValue());
                }
                stack.peek().appendChild(element);
                if (!start.selfClosing() && !VOID_ELEMENTS.contains(start.name())) {
                    stack.push(element);
                }
            } else if (token instanceof Token.EndTag end) {
                closeElement(stack, end.name());
            } else if (token instanceof Token.Characters chars) {
                stack.peek().appendChild(new Text(chars.data()));
            } else if (token instanceof Token.Comment comment) {
                stack.peek().appendChild(new Comment(comment.data()));
            }
            // Doctype and Eof carry no tree content.
        }
        return document;
    }

    private void closeElement(Deque<Node> stack, String name) {
        boolean matchOpen = false;
        for (Node n : stack) {
            if (n instanceof Element e && e.tagName().equals(name)) {
                matchOpen = true;
                break;
            }
        }
        if (!matchOpen) {
            return; // stray end tag
        }
        while (!stack.isEmpty()) {
            Node top = stack.peek();
            if (!(top instanceof Element e)) {
                break;
            }
            stack.pop();
            if (e.tagName().equals(name)) {
                break;
            }
        }
    }
}
