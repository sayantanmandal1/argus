package io.argus.browser.js;

import io.argus.browser.dom.Document;
import io.argus.browser.dom.Element;
import io.argus.browser.dom.Node;
import io.argus.browser.dom.Text;
import java.util.ArrayList;
import java.util.List;

/**
 * The JavaScript {@code document} object. Exposes the query and factory methods scripts rely on
 * ({@code getElementById}, {@code querySelector(All)}, {@code getElementsByTagName/ClassName},
 * {@code createElement}, {@code createTextNode}) plus {@code body}, {@code head},
 * {@code documentElement}, and a read/write {@code title}.
 */
public final class DomDocument extends JsObject {

    private final DomBinding binding;
    private final Document document;

    DomDocument(DomBinding binding, Document document) {
        this.binding = binding;
        this.document = document;
    }

    @Override
    public Object get(String name) {
        switch (name) {
            case "getElementById":
                return (JsCallable) (in, t, a) -> binding.wrap(document.getElementById(str(a, 0)));
            case "querySelector":
                return (JsCallable) (in, t, a) -> binding.wrap(DomBinding.first(document, str(a, 0)));
            case "querySelectorAll":
                return (JsCallable) (in, t, a) -> binding.elementArray(DomBinding.all(document, str(a, 0)));
            case "getElementsByTagName":
                return (JsCallable) (in, t, a) -> binding.elementArray(document.getElementsByTagName(str(a, 0)));
            case "getElementsByClassName":
                return (JsCallable) (in, t, a) -> binding.elementArray(DomBinding.all(document, "." + str(a, 0)));
            case "createElement":
                return (JsCallable) (in, t, a) -> binding.wrap(new Element(str(a, 0)));
            case "createTextNode":
                return (JsCallable) (in, t, a) -> binding.wrap(new Text(str(a, 0)));
            case "body":
                return binding.wrap(firstByTag("body"));
            case "head":
                return binding.wrap(firstByTag("head"));
            case "documentElement":
                return binding.wrap(document.documentElement());
            case "title":
                return title();
            default:
                return super.get(name);
        }
    }

    @Override
    public void set(String name, Object value) {
        if (name.equals("title")) {
            Element titleEl = firstByTag("title");
            if (titleEl != null) {
                for (Node child : new ArrayList<>(titleEl.childNodes())) {
                    titleEl.removeChild(child);
                }
                titleEl.appendChild(new Text(JsValues.stringify(value)));
            }
            return;
        }
        super.set(name, value);
    }

    private Element firstByTag(String tag) {
        List<Element> list = document.getElementsByTagName(tag);
        return list.isEmpty() ? null : list.get(0);
    }

    private String title() {
        Element titleEl = firstByTag("title");
        return titleEl == null ? "" : titleEl.textContent().trim();
    }

    private static String str(List<Object> args, int i) {
        return i < args.size() ? JsValues.stringify(args.get(i)) : "";
    }
}
