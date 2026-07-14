package io.argus.server;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A thread-safe fixed-window rate limiter keyed by client (typically IP). Each key gets at most
 * {@code maxPerWindow} requests per {@code windowMillis}; a zero or negative cap disables limiting.
 * Fixed windows are simple and allocation-light — a good fit for a lightweight embedded server.
 */
final class RateLimiter {

    private final int maxPerWindow;
    private final long windowMillis;
    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();

    RateLimiter(int maxPerWindow, long windowMillis) {
        this.maxPerWindow = maxPerWindow;
        this.windowMillis = windowMillis;
    }

    boolean allow(String client) {
        if (maxPerWindow <= 0) {
            return true;
        }
        Counter c = counters.computeIfAbsent(client, k -> new Counter());
        synchronized (c) {
            long now = System.currentTimeMillis();
            if (now - c.windowStart >= windowMillis) {
                c.windowStart = now;
                c.count = 0;
            }
            c.count++;
            return c.count <= maxPerWindow;
        }
    }

    private static final class Counter {
        long windowStart = System.currentTimeMillis();
        int count;
    }
}
