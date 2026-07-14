package io.argus.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.argus.index.PersistentIndex;
import io.argus.store.RAMDirectory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SecurityTest {

    private ArgusServer server;
    private final HttpClient client = HttpClient.newHttpClient();

    private String start(SecurityConfig cfg) throws Exception {
        PersistentIndex index = PersistentIndex.open(new RAMDirectory());
        server = new ArgusServer(0, new SearchService(index, "body"), cfg);
        server.start();
        return "http://localhost:" + server.port();
    }

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop();
        }
    }

    private int status(HttpRequest req) throws Exception {
        return client.send(req, HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    private static HttpRequest.Builder req(String url) {
        return HttpRequest.newBuilder(URI.create(url));
    }

    private static HttpRequest.BodyPublisher json(String body) {
        return HttpRequest.BodyPublishers.ofString(body);
    }

    @Test
    void writeRequiresApiKey() throws Exception {
        String base = start(SecurityConfig.builder().apiKey("secret-key").build());
        String body = "{\"body\":\"hello\"}";
        assertEquals(401, status(req(base + "/index").POST(json(body)).build()));
        assertEquals(401, status(req(base + "/index").header("Authorization", "Bearer wrong").POST(json(body)).build()));
        assertEquals(200, status(req(base + "/index").header("Authorization", "Bearer secret-key").POST(json(body)).build()));
    }

    @Test
    void readsArePublicByDefault() throws Exception {
        String base = start(SecurityConfig.builder().apiKey("k").build());
        assertEquals(200, status(req(base + "/search?q=hello").GET().build()));
    }

    @Test
    void securityHeadersArePresent() throws Exception {
        String base = start(SecurityConfig.dev());
        HttpResponse<String> r = client.send(req(base + "/stats").GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals("nosniff", r.headers().firstValue("X-Content-Type-Options").orElse(""));
        assertEquals("DENY", r.headers().firstValue("X-Frame-Options").orElse(""));
        assertTrue(r.headers().firstValue("Content-Security-Policy").isPresent());
    }

    @Test
    void uiUsesNonceBasedCsp() throws Exception {
        String base = start(SecurityConfig.dev());
        HttpResponse<String> r = client.send(req(base + "/").GET().build(), HttpResponse.BodyHandlers.ofString());
        String csp = r.headers().firstValue("Content-Security-Policy").orElse("");
        assertTrue(csp.contains("nonce-"), "UI CSP should use a nonce");
        assertTrue(r.body().contains("nonce="), "inline script/style should carry the nonce");
    }

    @Test
    void oversizedBodyIsRejected() throws Exception {
        String base = start(SecurityConfig.builder().apiKey("k").maxBodyBytes(40).build());
        String big = "{\"body\":\"" + "x".repeat(200) + "\"}";
        assertEquals(413, status(req(base + "/index").header("Authorization", "Bearer k").POST(json(big)).build()));
    }

    @Test
    void rateLimitReturns429() throws Exception {
        String base = start(SecurityConfig.builder().rateLimitPerMinute(3).build());
        int last = 200;
        for (int i = 0; i < 5; i++) {
            last = status(req(base + "/stats").GET().build());
        }
        assertEquals(429, last);
    }
}
