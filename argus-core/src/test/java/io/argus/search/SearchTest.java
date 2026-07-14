package io.argus.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.argus.document.Document;
import io.argus.index.IndexReader;
import io.argus.index.IndexWriter;
import io.argus.query.MatchAllDocsQuery;
import io.argus.query.TermQuery;
import org.junit.jupiter.api.Test;

class SearchTest {

    private IndexWriter indexed() {
        IndexWriter w = new IndexWriter();
        w.addDocument(new Document().addKeyword("id", "long").addText("body", "quick brown fox jumps"));
        w.addDocument(new Document().addKeyword("id", "short").addText("body", "fox"));
        w.addDocument(new Document().addKeyword("id", "other").addText("body", "quick brown dog"));
        return w;
    }

    @Test
    void shorterDocumentWithSameTermRanksHigher() {
        IndexSearcher s = new IndexSearcher(indexed().getReader());
        TopDocs td = s.search(new TermQuery("body", "fox"), 10);
        assertEquals(2, td.totalHits);
        assertEquals("short", s.doc(td.scoreDocs[0].docId).get("id"));
        assertEquals("long", s.doc(td.scoreDocs[1].docId).get("id"));
        assertTrue(td.scoreDocs[0].score > td.scoreDocs[1].score);
    }

    @Test
    void missingTermReturnsNoHits() {
        IndexSearcher s = new IndexSearcher(indexed().getReader());
        assertEquals(0, s.search(new TermQuery("body", "zebra"), 10).totalHits);
    }

    @Test
    void matchAllReturnsEveryLiveDocument() {
        IndexSearcher s = new IndexSearcher(indexed().getReader());
        assertEquals(3, s.search(new MatchAllDocsQuery(), 10).totalHits);
    }

    @Test
    void deletedDocumentsAreNotSearchable() {
        IndexWriter w = indexed();
        w.deleteByTerm("id", "short");
        IndexSearcher s = new IndexSearcher(w.getReader());
        TopDocs td = s.search(new TermQuery("body", "fox"), 10);
        assertEquals(1, td.totalHits);
        assertEquals("long", s.doc(td.scoreDocs[0].docId).get("id"));
    }

    @Test
    void rarerTermScoresHigherThanCommonTerm() {
        // "jump" occurs in 1 of 3 docs; "brown" in 2 of 3 -> "jump" has higher idf.
        IndexReader r = indexed().getReader();
        IndexSearcher s = new IndexSearcher(r);
        double jump = bestScoreFor(s, "jump");
        double brown = bestScoreFor(s, "brown");
        assertTrue(jump > brown, "rarer term should score higher via idf");
    }

    @Test
    void topKLimitsReturnedHitsButNotTotalCount() {
        IndexSearcher s = new IndexSearcher(indexed().getReader());
        TopDocs td = s.search(new TermQuery("body", "quick"), 1);
        assertEquals(2, td.totalHits);
        assertEquals(1, td.scoreDocs.length);
    }

    private double bestScoreFor(IndexSearcher s, String term) {
        TopDocs td = s.search(new TermQuery("body", term), 10);
        double max = 0;
        for (ScoreDoc sd : td.scoreDocs) {
            max = Math.max(max, sd.score);
        }
        return max;
    }
}
