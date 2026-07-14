package io.argus.index;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.argus.document.Document;
import io.argus.query.TermQuery;
import io.argus.search.IndexSearcher;
import io.argus.search.TopDocs;
import io.argus.store.ByteArrayIndexInput;
import io.argus.store.CorruptIndexException;
import io.argus.store.IndexInput;
import io.argus.store.IndexOutput;
import io.argus.store.RAMDirectory;
import org.junit.jupiter.api.Test;

class SegmentRoundTripTest {

    private IndexWriter build() {
        IndexWriter w = new IndexWriter();
        w.addDocument(new Document().addKeyword("id", "d0")
                .addText("body", "the quick brown fox").addStored("url", "http://a"));
        w.addDocument(new Document().addKeyword("id", "d1").addText("body", "quick brown dogs run fast"));
        w.addDocument(new Document().addKeyword("id", "d2").addText("body", "lazy brown fox sleeps"));
        w.deleteByTerm("id", "d1");
        return w;
    }

    @Test
    void roundTripsAllReaderState() {
        IndexReader original = build().getReader();
        RAMDirectory dir = new RAMDirectory();
        SegmentWriter.write(dir, "seg_1", original);
        IndexReader loaded = SegmentReader.load(dir, "seg_1");

        assertEquals(original.maxDoc(), loaded.maxDoc());
        assertEquals(original.numDocs(), loaded.numDocs());
        assertTrue(loaded.isDeleted(1));
        assertFalse(loaded.isDeleted(0));

        assertEquals(original.docFrequency("body", "brown"), loaded.docFrequency("body", "brown"));
        assertArrayEquals(original.postings("body", "fox").find(0).positions(),
                loaded.postings("body", "fox").find(0).positions());

        assertEquals("d0", loaded.document(0).get("id"));
        assertEquals("http://a", loaded.document(0).get("url"));

        assertEquals(original.fieldLength(0, "body"), loaded.fieldLength(0, "body"));
        assertEquals(original.averageFieldLength("body"), loaded.averageFieldLength("body"), 1e-9);
    }

    @Test
    void searchWorksOverLoadedSegment() {
        RAMDirectory dir = new RAMDirectory();
        SegmentWriter.write(dir, "seg_1", build().getReader());
        IndexReader loaded = SegmentReader.load(dir, "seg_1");

        IndexSearcher s = new IndexSearcher(loaded);
        TopDocs td = s.search(new TermQuery("body", "fox"), 10);
        assertEquals(2, td.totalHits); // d0 and d2 have fox; d1 is deleted
    }

    @Test
    void detectsCorruptSegment() {
        RAMDirectory dir = new RAMDirectory();
        SegmentWriter.write(dir, "seg_1", build().getReader());

        byte[] bytes;
        try (IndexInput in = dir.openInput("seg_1.post")) {
            bytes = new byte[(int) in.length()];
            in.readBytes(bytes, 0, bytes.length);
        }
        bytes[5] ^= 0xFF; // corrupt a content byte, keep original footer
        dir.deleteFile("seg_1.post");
        try (IndexOutput out = dir.createOutput("seg_1.post")) {
            out.writeBytes(bytes, 0, bytes.length);
        }

        assertThrows(CorruptIndexException.class, () -> SegmentReader.load(dir, "seg_1"));
        // sanity: the raw bytes really are shorter than a fresh read would checksum-verify
        assertTrue(new ByteArrayIndexInput(bytes).length() > 8);
    }
}
