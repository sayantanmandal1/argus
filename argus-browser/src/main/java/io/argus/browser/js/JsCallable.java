package io.argus.browser.js;

import java.util.List;

/** Anything callable from JavaScript: user-defined functions and native (Java-backed) functions. */
public interface JsCallable {

    Object call(Interpreter interpreter, Object thisValue, List<Object> arguments);
}
