package io.argus.index;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.argus.document.Document;
import io.argus.query.TermQuery;
import io.argus.search.IndexSearcher;
import io.argus.store.FSDirectory;
import io.argus.store.RAMDirectory;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PersistentIndexTest {

    private void add(PersistentIndex idx, String id, String body) {
        idx.addDocument(new Document().addKeyword("id", id).addText("body", body));
    }

    private int hits(PersistentIndex idx, String term) {
        return (int) new IndexSearcher(idx.getReader())
                .search(new TermQuery("body", term), 100).totalHits;
    }

    @Test
    void commitThenReopenLoadsCommittedDocs(@TempDir Path tmp) {
        PersistentIndex idx = PersistentIndex.open(new FSDirectory(tmp));
        add(idx, "d0", "distributed fault tolerant storage");
        add(idx, "d1", "scalable query engine");
        idx.commit();
        idx.close();

        PersistentIndex reopened = PersistentIndex.open(new FSDirectory(tmp));
        assertEquals(2, reopened.numDocs());
        assertEquals(1, hits(reopened, "storag"));
        assertEquals(1, hits(reopened, "engin"));
        reopened.close();
    }

    @Test
    void crashBeforeCommitRecoversFromWal() {
        RAMDirectory dir = new RAMDirectory();
        PersistentIndex idx = PersistentIndex.open(dir);
        add(idx, "d0", "alpha beta gamma");
        add(idx, "d1", "beta gamma delta");
        add(idx, "d2", "gamma delta epsilon");
        // Simulate a crash: no commit, no clean close.

        PersistentIndex recovered = PersistentIndex.open(dir);
        assertEquals(3, recovered.numDocs());
        assertEquals(3, hits(recovered, "gamma"));
        assertEquals(2, hits(recovered, "delta"));
        recovered.close();
    }

    @Test
    void committedThenCrashOnLaterAddsRecoversBoth() {
        RAMDirectory dir = new RAMDirectory();
        PersistentIndex idx = PersistentIndex.open(dir);
        add(idx, "d0", "committed document one");
        idx.commit();
        add(idx, "d1", "uncommitted document two"); // logged but not yet committed

        PersistentIndex recovered = PersistentIndex.open(dir);
        assertEquals(2, recovered.numDocs());
        assertEquals(1, hits(recovered, "commit"));
        assertEquals(1, hits(recovered, "uncommit"));
        recovered.close();
    }

    @Test
    void deletesArePersistedAcrossCommitAndReopen(@TempDir Path tmp) {
        PersistentIndex idx = PersistentIndex.open(new FSDirectory(tmp));
        add(idx, "d0", "fault tolerant");
        add(idx, "d1", "fault isolation");
        idx.commit();
        idx.deleteByTerm("id", "d0");
        idx.commit();
        idx.close();

        PersistentIndex reopened = PersistentIndex.open(new FSDirectory(tmp));
        assertEquals(1, reopened.numDocs());
        assertEquals(1, hits(reopened, "fault"));
        reopened.close();
    }

    @Test
    void walIsClearedAfterCommit() {
        RAMDirectory dir = new RAMDirectory();
        PersistentIndex idx = PersistentIndex.open(dir);
        add(idx, "d0", "alpha");
        idx.commit();
        idx.close();
        assertEquals(0, WriteAheadLog.replay(dir, "wal.log").size());
    }
}
