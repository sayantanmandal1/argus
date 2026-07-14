package io.argus.browser.css;

/** A single CSS declaration, e.g. {@code color: red}. */
public record Declaration(String property, String value) {
}
