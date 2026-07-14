package io.argus.browser.net;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * A {@link ResourceLoader} backed by the JDK HTTP client. It follows redirects and honors the
 * response charset, and applies a few defensive limits appropriate for a client that fetches
 * arbitrary user-entered URLs:
 *
 * <ul>
 *   <li>only {@code http}/{@code https} are allowed (no {@code file:} or other local schemes);</li>
 *   <li>connect and request timeouts bound how long a hostile server can stall us;</li>
 *   <li>the response body is capped so a huge download cannot exhaust memory.</li>
 * </ul>
 *
 * <p>TLS certificate validation is left at the JDK default (enabled) — it is never disabled.
 */
public final class HttpResourceLoader implements ResourceLoader {

    private static final long MAX_BYTES = 10_000_000L;
    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private final HttpClient client;

    public HttpResourceLoader() {
        this.client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String fetchText(URI uri) throws IOException {
        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))) {
            throw new IOException("Unsupported URL scheme: " + scheme);
        }
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("User-Agent", "ArgusBrowser/1.0 (+https://github.com/argus)")
                .header("Accept", "text/html,application/xhtml+xml,text/css,*/*")
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
        try {
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            byte[] body = response.body();
            if (body.length > MAX_BYTES) {
                throw new IOException("Response body exceeds " + MAX_BYTES + " bytes");
            }
            Charset charset = charsetOf(response).orElse(StandardCharsets.UTF_8);
            return new String(body, charset);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while fetching " + uri, e);
        }
    }

    private static Optional<Charset> charsetOf(HttpResponse<?> response) {
        return response.headers().firstValue("content-type").flatMap(HttpResourceLoader::charsetFromContentType);
    }

    private static Optional<Charset> charsetFromContentType(String contentType) {
        for (String part : contentType.split(";")) {
            String token = part.trim();
            if (token.toLowerCase(Locale.ROOT).startsWith("charset=")) {
                String name = token.substring("charset=".length()).trim().replace("\"", "");
                try {
                    return Optional.of(Charset.forName(name));
                } catch (RuntimeException ignored) {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }
}
