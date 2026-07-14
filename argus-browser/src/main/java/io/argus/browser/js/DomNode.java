package io.argus.browser.js;

import io.argus.browser.dom.Comment;
import io.argus.browser.dom.Document;
import io.argus.browser.dom.Element;
import io.argus.browser.dom.Node;
import io.argus.browser.dom.Text;
import io.argus.browser.html.HtmlParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A JavaScript wrapper over a DOM {@link Node}. Special properties ({@code textContent},
 * {@code innerHTML}, {@code id}, {@code className}, {@code children}, ...) project the live element,
 * and DOM methods ({@code getAttribute}, {@code appendChild}, {@code querySelector}, ...) are exposed
 * as native functions. Reading or writing these mutates the underlying DOM, which is what makes a
 * script's changes show up when the page is re-styled and repainted.
 */
public final class DomNode extends JsObject {

    private final DomBinding binding;
    final Node node;

    DomNode(DomBinding binding, Node node) {
        this.binding = binding;
        this.node = node;
    }

    private Element element() {
        return node instanceof Element e ? e : null;
    }

    @Override
    public Object get(String name) {
        Element el = element();
        switch (name) {
            case "tagName":
                return el != null ? el.tagName().toUpperCase(Locale.ROOT) : Undefined.VALUE;
            case "nodeName":
                return node.nodeName().startsWith("#") ? node.nodeName() : node.nodeName().toUpperCase(Locale.ROOT);
            case "nodeType":
                return nodeType();
            case "id":
                return el != null ? orEmpty(el.getAttribute("id")) : "";
            case "className":
                return el != null ? orEmpty(el.getAttribute("class")) : "";
            case "textContent":
                return node.textContent();
            case "innerHTML":
                return el != null ? serializeChildren(el) : node.textContent();
            case "outerHTML":
                return el != null ? serialize(el) : node.textContent();
            case "value":
                return el != null ? orEmpty(el.getAttribute("value")) : "";
            case "children":
                return binding.elementArray(el != null ? el.childElements() : List.of());
            case "childNodes":
                return nodeArray(node.childNodes());
            case "parentNode":
            case "parentElement":
                return binding.wrap(node.parent());
            case "firstChild":
                return node.childNodes().isEmpty() ? null : binding.wrap(node.childNodes().get(0));
            case "nextSibling":
                return sibling(1);
            case "previousSibling":
                return sibling(-1);
            case "style":
                return new DomStyle(el);
            case "getAttribute":
                return (JsCallable) (in, t, a) -> el == null ? null : el.getAttribute(str(a, 0));
            case "setAttribute":
                return (JsCallable) (in, t, a) -> {
                    if (el != null) {
                        el.setAttribute(str(a, 0), str(a, 1));
                    }
                    return Undefined.VALUE;
                };
            case "hasAttribute":
                return (JsCallable) (in, t, a) -> el != null && el.hasAttribute(str(a, 0));
            case "appendChild":
                return (JsCallable) (in, t, a) -> {
                    Node child = unwrap(a);
                    if (child != null) {
                        node.appendChild(child);
                    }
                    return a.isEmpty() ? Undefined.VALUE : a.get(0);
                };
            case "removeChild":
                return (JsCallable) (in, t, a) -> {
                    Node child = unwrap(a);
                    if (child != null) {
                        node.removeChild(child);
                    }
                    return a.isEmpty() ? Undefined.VALUE : a.get(0);
                };
            case "remove":
                return (JsCallable) (in, t, a) -> {
                    if (node.parent() != null) {
                        node.parent().removeChild(node);
                    }
                    return Undefined.VALUE;
                };
            case "getElementsByTagName":
                return (JsCallable) (in, t, a) -> binding.elementArray(node.getElementsByTagName(str(a, 0)));
            case "getElementById":
                return (JsCallable) (in, t, a) -> binding.wrap(node.getElementById(str(a, 0)));
            case "querySelector":
                return (JsCallable) (in, t, a) -> binding.wrap(DomBinding.first(node, str(a, 0)));
            case "querySelectorAll":
                return (JsCallable) (in, t, a) -> binding.elementArray(DomBinding.all(node, str(a, 0)));
            case "addEventListener":
            case "removeEventListener":
                return (JsCallable) (in, t, a) -> Undefined.VALUE; // events are not dispatched
            default:
                return super.get(name);
        }
    }

    @Override
    public void set(String name, Object value) {
        Element el = element();
        switch (name) {
            case "textContent" -> setTextContent(JsValues.stringify(value));
            case "innerHTML" -> {
                if (el != null) {
                    setInnerHtml(el, JsValues.stringify(value));
                }
            }
            case "id" -> {
                if (el != null) {
                    el.setAttribute("id", JsValues.stringify(value));
                }
            }
            case "className" -> {
                if (el != null) {
                    el.setAttribute("class", JsValues.stringify(value));
                }
            }
            case "value" -> {
                if (el != null) {
                    el.setAttribute("value", JsValues.stringify(value));
                }
            }
            default -> super.set(name, value);
        }
    }

    private Object nodeType() {
        if (node instanceof Element) {
            return 1.0;
        }
        if (node instanceof Text) {
            return 3.0;
        }
        if (node instanceof Comment) {
            return 8.0;
        }
        return 9.0;
    }

    private Object sibling(int direction) {
        Node parent = node.parent();
        if (parent == null) {
            return null;
        }
        List<Node> siblings = parent.childNodes();
        int i = siblings.indexOf(node);
        int j = i + direction;
        return i >= 0 && j >= 0 && j < siblings.size() ? binding.wrap(siblings.get(j)) : null;
    }

    private JsArray nodeArray(List<Node> nodes) {
        JsArray array = new JsArray();
        for (Node n : nodes) {
            array.items().add(binding.wrap(n));
        }
        return array;
    }

    private void setTextContent(String text) {
        for (Node child : new ArrayList<>(node.childNodes())) {
            node.removeChild(child);
        }
        node.appendChild(new Text(text));
    }

    private void setInnerHtml(Element el, String html) {
        for (Node child : new ArrayList<>(el.childNodes())) {
            el.removeChild(child);
        }
        Document fragment = new HtmlParser().parse(html);
        for (Node child : new ArrayList<>(fragment.childNodes())) {
            el.appendChild(child);
        }
    }

    private static String serialize(Element el) {
        StringBuilder sb = new StringBuilder("<").append(el.tagName());
        for (Map.Entry<String, String> attr : el.attributes().entrySet()) {
            sb.append(' ').append(attr.getKey()).append("=\"").append(attr.getValue()).append('"');
        }
        sb.append('>').append(serializeChildren(el)).append("</").append(el.tagName()).append('>');
        return sb.toString();
    }

    private static String serializeChildren(Element el) {
        StringBuilder sb = new StringBuilder();
        for (Node child : el.childNodes()) {
            if (child instanceof Element e) {
                sb.append(serialize(e));
            } else if (child instanceof Text t) {
                sb.append(t.data());
            } else if (child instanceof Comment c) {
                sb.append("<!--").append(c.textContent()).append("-->");
            }
        }
        return sb.toString();
    }

    private Node unwrap(List<Object> args) {
        return !args.isEmpty() && args.get(0) instanceof DomNode dn ? dn.node : null;
    }

    private static String orEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String str(List<Object> args, int i) {
        return i < args.size() ? JsValues.stringify(args.get(i)) : "";
    }
}
