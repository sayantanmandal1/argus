package io.argus.browser.js;

import io.argus.browser.dom.Document;
import java.util.List;

/**
 * Runs scripts against a document. It wires the {@code document} and a minimal {@code window} into
 * the interpreter's global scope, so a page's inline scripts can read and mutate the DOM before the
 * engine styles and paints it.
 */
public final class JsEngine {

    private final Interpreter interpreter = new Interpreter();
    private final DomBinding binding;

    public JsEngine(Document document) {
        this.binding = new DomBinding(document);
        JsObject documentObject = binding.documentObject();
        JsObject window = new JsObject();
        window.set("document", documentObject);
        interpreter.global().define("document", documentObject);
        interpreter.global().define("window", window);
        interpreter.global().define("globalThis", window);
        interpreter.global().define("alert",
                (JsCallable) (in, t, a) -> {
                    in.print(a.isEmpty() ? "" : JsValues.stringify(a.get(0)));
                    return Undefined.VALUE;
                });
    }

    public void run(String source) {
        interpreter.run(source);
    }

    public Interpreter interpreter() {
        return interpreter;
    }

    public List<String> consoleOutput() {
        return interpreter.consoleOutput();
    }
}
