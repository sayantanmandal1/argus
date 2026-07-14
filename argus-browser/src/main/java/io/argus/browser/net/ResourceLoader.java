package io.argus.browser.net;

import java.io.IOException;
import java.net.URI;

/**
 * Fetches the text of a resource (an HTML page or a linked stylesheet) given its absolute URL. An
 * interface so the engine can be driven by the real network in production and by a canned map in
 * tests, keeping rendering logic testable without hitting the network.
 */
public interface ResourceLoader {

    String fetchText(URI uri) throws IOException;
}
