package io.argus.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.argus.index.PersistentIndex;
import io.argus.store.RAMDirectory;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServerTest {

    private ArgusServer server;
    private HttpClient client;
    private String base;

    @BeforeEach
    void setUp() throws Exception {
        PersistentIndex index = PersistentIndex.open(new RAMDirectory());
        server = new ArgusServer(0, new SearchService(index, "body"));
        server.start();
        base = "http://localhost:" + server.port();
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    @Test
    void indexSearchDeleteStatsEndToEnd() throws Exception {
        assertEquals(200, post("/index",
                "{\"id\":\"d0\",\"title\":\"distributed systems\",\"body\":\"fault tolerant storage\"}").statusCode());
        post("/index",
                "{\"id\":\"d1\",\"title\":\"operating systems\",\"body\":\"fault isolation kernel\"}");
        post("/commit", "");

        HttpResponse<String> r = get("/search?q=fault&field=body&k=10");
        assertEquals(200, r.statusCode());
        assertEquals(2L, Json.parseObject(r.body()).get("total"));

        // field-qualified phrase query
        Map<String, Object> phrase = Json.parseObject(get("/search?q=" + enc("body:\"fault tolerant\"")).body());
        assertEquals(1L, phrase.get("total"));

        // delete d0 by its id term
        assertEquals(200, post("/delete", "{\"field\":\"id\",\"term\":\"d0\"}").statusCode());
        assertEquals(1L, Json.parseObject(get("/search?q=fault").body()).get("total"));

        Map<String, Object> stats = Json.parseObject(get("/stats").body());
        assertEquals(1L, ((Number) stats.get("numDocs")).longValue());
    }

    @Test
    void servesBrowserUi() throws Exception {
        HttpResponse<String> r = get("/");
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("Argus"));
        assertTrue(r.headers().firstValue("Content-Type").orElse("").contains("text/html"));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        assertEquals(400, post("/index", "{not valid json").statusCode());
    }
}
