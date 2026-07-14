package io.argus.browser.js;

/** The JavaScript {@code undefined} value, kept distinct from Java {@code null} ({@code null}). */
public final class Undefined {

    public static final Undefined VALUE = new Undefined();

    private Undefined() {
    }

    @Override
    public String toString() {
        return "undefined";
    }
}
