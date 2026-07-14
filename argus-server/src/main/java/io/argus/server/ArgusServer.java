package io.argus.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.argus.index.PersistentIndex;
import io.argus.store.FSDirectory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * A dependency-free HTTP front-end for the search engine, built on the JDK's {@code HttpServer}.
 *
 * <pre>
 *   POST /index    {"id":"d1","title":"...","body":"..."}   -&gt; {"docId":N}
 *   GET  /search?q=...&amp;field=body&amp;k=10                       -&gt; {"total":N,"hits":[...]}
 *   POST /delete   {"field":"id","term":"d1"}               -&gt; {"deleted":N}
 *   POST /commit                                            -&gt; {"status":"ok"}
 *   GET  /stats                                             -&gt; {"numDocs":N,...}
 *   GET  /                                                  -&gt; browser search UI
 * </pre>
 */
public final class ArgusServer {

    private static final int MAX_QUERY_LENGTH = 2048;
    private static final int MAX_HITS = 1000;
    private static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();

    private final HttpServer http;
    private final SearchService service;
    private final SecurityConfig security;
    private final RateLimiter rateLimiter;

    public ArgusServer(int port, SearchService service) throws IOException {
        this(port, service, SecurityConfig.dev());
    }

    public ArgusServer(int port, SearchService service, SecurityConfig security) throws IOException {
        this.service = service;
        this.security = security;
        this.rateLimiter = new RateLimiter(security.rateLimitPerMinute(), 60_000L);
        this.http = HttpServer.create(new InetSocketAddress(port), 0);
        http.createContext("/index", secure(true, this::handleIndex));
        http.createContext("/delete", secure(true, this::handleDelete));
        http.createContext("/commit", secure(true, this::handleCommit));
        http.createContext("/search", secure(security.protectReads(), this::handleSearch));
        http.createContext("/stats", secure(security.protectReads(), this::handleStats));
        http.createContext("/", secure(false, this::handleRoot));
        http.setExecutor(Executors.newCachedThreadPool());
    }

    public void start() {
        http.start();
    }

    public void stop() {
        http.stop(0);
    }

    public int port() {
        return http.getAddress().getPort();
    }

    // ------------------------------------------------------------------ handlers

    private void handleIndex(HttpExchange e) throws IOException {
        requireMethod(e, "POST");
        Map<String, Object> body = Json.parseObject(readBody(e));
        if (body.isEmpty()) {
            throw new IllegalArgumentException("document has no fields");
        }
        sendJson(e, 200, Map.of("docId", service.index(body)));
    }

    private void handleSearch(HttpExchange e) throws IOException {
        Map<String, String> params = queryParams(e);
        String q = params.getOrDefault("q", "");
        if (q.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException("query too long");
        }
        int k = clamp(parseInt(params.get("k"), 10), 1, MAX_HITS);
        sendJson(e, 200, service.search(q, params.get("field"), k));
    }

    private void handleDelete(HttpExchange e) throws IOException {
        requireMethod(e, "POST");
        Map<String, Object> body = Json.parseObject(readBody(e));
        sendJson(e, 200, Map.of("deleted",
                service.deleteByTerm(requireString(body, "field"), requireString(body, "term"))));
    }

    private void handleCommit(HttpExchange e) throws IOException {
        requireMethod(e, "POST");
        service.commit();
        sendJson(e, 200, Map.of("status", "ok"));
    }

    private void handleStats(HttpExchange e) throws IOException {
        sendJson(e, 200, service.stats());
    }

    private void handleRoot(HttpExchange e) throws IOException {
        String path = e.getRequestURI().getPath();
        if (path.equals("/") || path.equals("/ui")) {
            String nonce = newNonce();
            byte[] html = WebUi.render(nonce).getBytes(StandardCharsets.UTF_8);
            e.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            e.getResponseHeaders().add("Content-Security-Policy",
                    "default-src 'self'; script-src 'nonce-" + nonce + "'; style-src 'nonce-" + nonce
                            + "'; base-uri 'none'; form-action 'self'");
            e.sendResponseHeaders(200, html.length);
            try (OutputStream os = e.getResponseBody()) {
                os.write(html);
            }
        } else {
            sendJson(e, 404, Map.of("error", "not found"));
        }
    }

    // ------------------------------------------------------------------ security + plumbing

    @FunctionalInterface
    private interface Route {
        void handle(HttpExchange e) throws Exception;
    }

    private HttpHandler secure(boolean requiresAuth, Route route) {
        return exchange -> {
            addSecurityHeaders(exchange);
            try {
                if (!rateLimiter.allow(clientIp(exchange))) {
                    safeSend(exchange, 429, err("rate limit exceeded"));
                    return;
                }
                if (requiresAuth && !authorized(exchange)) {
                    exchange.getResponseHeaders().add("WWW-Authenticate", "Bearer");
                    safeSend(exchange, 401, err("unauthorized"));
                    return;
                }
                route.handle(exchange);
            } catch (PayloadTooLargeException ex) {
                safeSend(exchange, 413, err("payload too large"));
            } catch (Json.JsonException | IllegalArgumentException ex) {
                safeSend(exchange, 400, err(ex.getMessage()));
            } catch (Exception ex) {
                safeSend(exchange, 500, err("internal error"));
            } finally {
                exchange.close();
            }
        };
    }

    private void addSecurityHeaders(HttpExchange e) {
        var h = e.getResponseHeaders();
        h.add("X-Content-Type-Options", "nosniff");
        h.add("X-Frame-Options", "DENY");
        h.add("Referrer-Policy", "no-referrer");
        h.add("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
        h.add("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        if (security.allowedOrigin() != null) {
            h.add("Access-Control-Allow-Origin", security.allowedOrigin());
            h.add("Vary", "Origin");
        }
    }

    private boolean authorized(HttpExchange e) {
        java.util.Set<String> keys = security.apiKeys();
        if (keys.isEmpty()) {
            return true; // authentication disabled
        }
        String token = bearerToken(e);
        if (token == null) {
            return false;
        }
        for (String key : keys) {
            if (constantTimeEquals(key, token)) {
                return true;
            }
        }
        return false;
    }

    private static String bearerToken(HttpExchange e) {
        String auth = e.getRequestHeaders().getFirst("Authorization");
        if (auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return auth.substring(7).trim();
        }
        String key = e.getRequestHeaders().getFirst("X-API-Key");
        return key == null ? null : key.trim();
    }

    private static boolean constantTimeEquals(String a, String b) {
        return java.security.MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static String clientIp(HttpExchange e) {
        InetSocketAddress a = e.getRemoteAddress();
        return (a == null || a.getAddress() == null) ? "unknown" : a.getAddress().getHostAddress();
    }

    private static String newNonce() {
        byte[] b = new byte[16];
        RANDOM.nextBytes(b);
        return java.util.Base64.getEncoder().encodeToString(b);
    }

    private String readBody(HttpExchange e) throws IOException {
        try (InputStream in = e.getRequestBody()) {
            int limit = security.maxBodyBytes();
            byte[] data = in.readNBytes(limit + 1);
            if (data.length > limit) {
                throw new PayloadTooLargeException();
            }
            return new String(data, StandardCharsets.UTF_8);
        }
    }

    private void sendJson(HttpExchange e, int status, Object body) throws IOException {
        byte[] bytes = Json.write(body).getBytes(StandardCharsets.UTF_8);
        var h = e.getResponseHeaders();
        h.add("Content-Type", "application/json; charset=utf-8");
        h.add("Content-Security-Policy", "default-src 'none'");
        e.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = e.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void safeSend(HttpExchange e, int status, Object body) {
        try {
            sendJson(e, status, body);
        } catch (IOException ignore) {
            // client went away; nothing to do
        }
    }

    private static Map<String, Object> err(String message) {
        return Map.of("error", message == null ? "error" : message);
    }

    private static String requireString(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null || String.valueOf(v).isBlank()) {
            throw new IllegalArgumentException("missing field: " + key);
        }
        return String.valueOf(v);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static final class PayloadTooLargeException extends RuntimeException {
    }

    private static Map<String, String> queryParams(HttpExchange e) {
        Map<String, String> m = new LinkedHashMap<>();
        String raw = e.getRequestURI().getRawQuery();
        if (raw == null || raw.isEmpty()) {
            return m;
        }
        for (String pair : raw.split("&")) {
            int i = pair.indexOf('=');
            if (i < 0) {
                m.put(decode(pair), "");
            } else {
                m.put(decode(pair.substring(0, i)), decode(pair.substring(i + 1)));
            }
        }
        return m;
    }

    private static String decode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    private static int parseInt(String s, int fallback) {
        if (s == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static void requireMethod(HttpExchange e, String method) {
        if (!e.getRequestMethod().equalsIgnoreCase(method)) {
            throw new IllegalArgumentException("method not allowed: " + e.getRequestMethod());
        }
    }

    /** Launches a durable server: {@code java ... ArgusServer [port] [data-dir]}. Set the
     *  {@code ARGUS_API_KEY} environment variable to require a bearer token on write endpoints. */
    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        String dataDir = args.length > 1 ? args[1] : "argus-data";
        SecurityConfig security = SecurityConfig.builder()
                .apiKey(System.getenv("ARGUS_API_KEY"))
                .rateLimitPerMinute(600)
                .build();
        PersistentIndex index = PersistentIndex.open(new FSDirectory(Paths.get(dataDir)));
        ArgusServer server = new ArgusServer(port, new SearchService(index, "body"), security);
        server.start();
        System.out.println("Argus is searching on http://localhost:" + server.port() + "  (UI at /)");
        if (!security.apiKeys().isEmpty()) {
            System.out.println("Write endpoints require:  Authorization: Bearer <ARGUS_API_KEY>");
        }
    }
}
