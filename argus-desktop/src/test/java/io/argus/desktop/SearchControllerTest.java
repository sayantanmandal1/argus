package io.argus.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.argus.index.PersistentIndex;
import io.argus.server.SearchService;
import io.argus.store.RAMDirectory;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SearchControllerTest {

    private SearchController controller() {
        PersistentIndex idx = PersistentIndex.open(new RAMDirectory());
        SearchService service = new SearchService(idx, "body");
        service.index(Map.of("id", "d0", "body", "fault tolerant storage"));
        service.index(Map.of("id", "d1", "body", "fault isolation kernel"));
        service.index(Map.of("id", "d2", "body", "scalable query engine"));
        service.commit();
        return new SearchController(service);
    }

    @Test
    void searchReturnsHits() {
        assertEquals(2L, controller().search("fault").get("total"));
    }

    @Test
    void blankQueryBrowsesAllDocuments() {
        assertEquals(3L, controller().search("").get("total"));
    }

    @Test
    void backAndForwardNavigateHistory() {
        SearchController c = controller();
        c.search("fault");
        c.search("engine");
        assertTrue(c.canGoBack());
        assertFalse(c.canGoForward());

        Map<String, Object> back = c.goBack();
        assertNotNull(back);
        assertEquals("fault", c.currentQuery());
        assertTrue(c.canGoForward());

        c.goForward();
        assertEquals("engine", c.currentQuery());
    }

    @Test
    void indexingMakesDocumentSearchable() {
        SearchController c = controller();
        c.index(Map.of("id", "d3", "body", "distributed consensus raft"));
        c.commit();
        assertEquals(1L, c.search("raft").get("total"));
    }

    @Test
    void statsReportsDocumentCount() {
        assertEquals(3L, ((Number) controller().stats().get("numDocs")).longValue());
    }
}
