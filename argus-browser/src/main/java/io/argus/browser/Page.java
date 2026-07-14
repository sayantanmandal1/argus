package io.argus.browser;

import io.argus.browser.dom.Document;
import io.argus.browser.style.StyledNode;
import java.net.URI;

/** A loaded page: its URL, the parsed DOM, the styled (cascaded) tree, and the document title. */
public record Page(URI url, Document document, StyledNode styledRoot, String title) {
}
