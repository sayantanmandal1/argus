package io.argus.browser.layout;

import io.argus.browser.style.StyledNode;

/** A single positioned word produced by inline layout, ready for the paint stage to draw. */
public record InlineFragment(String text, StyledNode style, Rect box) {
}
