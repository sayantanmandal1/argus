package io.argus.browser.layout;

import io.argus.browser.style.StyledNode;

/** A contiguous run of inline text together with the styled node it inherits its font/color from. */
public record TextRun(String text, StyledNode style) {
}
