package io.argus.desktop;

import io.argus.server.SearchService;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;

/**
 * The non-GUI heart of the desktop app: it runs searches through the {@link SearchService} and keeps
 * a back/forward history of queries (like a browser). Kept free of Swing so it can be unit-tested
 * headlessly.
 */
public final class SearchController {

    private static final int PAGE_SIZE = 20;

    private final SearchService service;
    private final Deque<String> back = new ArrayDeque<>();
    private final Deque<String> forward = new ArrayDeque<>();
    private String current;

    public SearchController(SearchService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    /** Runs a query, recording the previous one in history. */
    public Map<String, Object> search(String query) {
        String q = query == null ? "" : query;
        if (current != null && !current.equals(q)) {
            back.push(current);
            forward.clear();
        }
        current = q;
        return service.search(q, null, PAGE_SIZE);
    }

    public boolean canGoBack() {
        return !back.isEmpty();
    }

    public boolean canGoForward() {
        return !forward.isEmpty();
    }

    /** Navigates to the previous query, or returns {@code null} if there is none. */
    public Map<String, Object> goBack() {
        if (back.isEmpty()) {
            return null;
        }
        forward.push(current);
        current = back.pop();
        return service.search(current, null, PAGE_SIZE);
    }

    /** Navigates to the next query, or returns {@code null} if there is none. */
    public Map<String, Object> goForward() {
        if (forward.isEmpty()) {
            return null;
        }
        back.push(current);
        current = forward.pop();
        return service.search(current, null, PAGE_SIZE);
    }

    public String currentQuery() {
        return current;
    }

    public int index(Map<String, Object> document) {
        return service.index(document);
    }

    public void commit() {
        service.commit();
    }

    public Map<String, Object> stats() {
        return service.stats();
    }
}
