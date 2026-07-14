package io.argus.browser.js;

/** Runtime or parse error raised by the JavaScript engine. */
public class JsException extends RuntimeException {

    public JsException(String message) {
        super(message);
    }

    public JsException(String message, Throwable cause) {
        super(message, cause);
    }
}
