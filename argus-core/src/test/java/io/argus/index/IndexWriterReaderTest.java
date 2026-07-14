package io.argus.index;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.argus.document.Document;
import org.junit.jupiter.api.Test;

class IndexWriterReaderTest {

    @Test
    void indexesTermsWithDocFrequency() {
        IndexWriter w = new IndexWriter();
        w.addDocument(new Document().addText("body", "the quick brown fox"));
        w.addDocument(new Document().addText("body", "the lazy brown dog"));
        IndexReader r = w.getReader();

        assertEquals(2, r.maxDoc());
        assertEquals(2, r.numDocs());
        assertEquals(2, r.docFrequency("body", "brown"));
        assertEquals(1, r.docFrequency("body", "quick"));
        assertEquals(0, r.docFrequency("body", "the"), "stop word must not be indexed");
    }

    @Test
    void recordsPositionsWithStopWordGaps() {
        IndexWriter w = new IndexWriter();
        w.addDocument(new Document().addText("body", "the quick brown fox"));
        IndexReader r = w.getReader();

        // "the" is dropped but leaves a position: quick@1, brown@2, fox@3
        assertArrayEquals(new int[] {1}, r.postings("body", "quick").find(0).positions());
        assertArrayEquals(new int[] {2}, r.postings("body", "brown").find(0).positions());
        assertArrayEquals(new int[] {3}, r.postings("body", "fox").find(0).positions());
    }

    @Test
    void tracksFieldLengthAndAverage() {
        IndexWriter w = new IndexWriter();
        w.addDocument(new Document().addText("body", "quick brown fox"));   // 3 tokens
        w.addDocument(new Document().addText("body", "brown fox jumps high")); // 4 tokens
        IndexReader r = w.getReader();

        assertEquals(3, r.fieldLength(0, "body"));
        assertEquals(4, r.fieldLength(1, "body"));
        assertEquals(3.5, r.averageFieldLength("body"));
        assertEquals(7L, r.totalFieldLength("body"));
    }

    @Test
    void termFrequencyCountsRepeats() {
        IndexWriter w = new IndexWriter();
        w.addDocument(new Document().addText("body", "buffalo buffalo buffalo herd"));
        IndexReader r = w.getReader();
        Posting p = r.postings("body", "buffalo").find(0);
        assertEquals(3, p.frequency());
        assertArrayEquals(new int[] {0, 1, 2}, p.positions());
    }

    @Test
    void keywordFieldsAreNotAnalyzed() {
        IndexWriter w = new IndexWriter();
        w.addDocument(new Document().addKeyword("status", "In Progress"));
        IndexReader r = w.getReader();
        // Whole value is a single term; it is not tokenized or lowercased.
        assertEquals(1, r.docFrequency("status", "In Progress"));
        assertEquals(0, r.docFrequency("status", "progress"));
    }

    @Test
    void softDeleteHidesDocuments() {
        IndexWriter w = new IndexWriter();
        w.addDocument(new Document().addKeyword("id", "a").addText("body", "hello world"));
        w.addDocument(new Document().addKeyword("id", "b").addText("body", "hello there"));

        assertEquals(2, w.numDocs());
        assertEquals(1, w.deleteByTerm("id", "a"));

        IndexReader r = w.getReader();
        assertEquals(2, r.maxDoc());
        assertEquals(1, r.numDocs());
        assertTrue(r.isDeleted(0));
        assertFalse(r.isDeleted(1));
        // "hello" postings still physically present; search layer filters deleted docs.
        assertEquals(2, r.docFrequency("body", "hello"));
    }

    @Test
    void storesAndRetrievesDocuments() {
        IndexWriter w = new IndexWriter();
        w.addDocument(new Document()
                .addKeyword("id", "doc-1")
                .addText("title", "Distributed Systems")
                .addStored("url", "https://example.com/1"));
        IndexReader r = w.getReader();

        Document d = r.document(0);
        assertEquals("doc-1", d.get("id"));
        assertEquals("Distributed Systems", d.get("title"));
        assertEquals("https://example.com/1", d.get("url"));
        // Stored-only field is retrievable but not searchable.
        assertEquals(0, r.docFrequency("url", "https"));
        assertNull(r.postings("url", "https"));
    }
}
