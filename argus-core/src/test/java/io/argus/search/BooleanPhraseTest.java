package io.argus.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.argus.document.Document;
import io.argus.index.IndexWriter;
import io.argus.query.BooleanQuery;
import io.argus.query.PhraseQuery;
import io.argus.query.PrefixQuery;
import io.argus.query.TermQuery;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

class BooleanPhraseTest {

    private IndexSearcher searcher() {
        IndexWriter w = new IndexWriter();
        w.addDocument(new Document().addKeyword("id", "d0").addText("body", "the quick brown fox"));
        w.addDocument(new Document().addKeyword("id", "d1").addText("body", "quick brown dogs"));
        w.addDocument(new Document().addKeyword("id", "d2").addText("body", "lazy brown fox"));
        w.addDocument(new Document().addKeyword("id", "d3").addText("body", "quick red fox"));
        return new IndexSearcher(w.getReader());
    }

    private Set<String> ids(IndexSearcher s, io.argus.search.Query q) {
        TopDocs td = s.search(q, 100);
        Set<String> out = new TreeSet<>();
        for (ScoreDoc sd : td.scoreDocs) {
            out.add(s.doc(sd.docId).get("id"));
        }
        return out;
    }

    @Test
    void mustIntersects() {
        IndexSearcher s = searcher();
        BooleanQuery q = new BooleanQuery.Builder()
                .must(new TermQuery("body", "quick"))
                .must(new TermQuery("body", "fox"))
                .build();
        assertEquals(Set.of("d0", "d3"), ids(s, q));
    }

    @Test
    void shouldUnions() {
        IndexSearcher s = searcher();
        BooleanQuery q = new BooleanQuery.Builder()
                .should(new TermQuery("body", "quick"))
                .should(new TermQuery("body", "lazi"))
                .build();
        assertEquals(Set.of("d0", "d1", "d2", "d3"), ids(s, q));
    }

    @Test
    void mustNotExcludes() {
        IndexSearcher s = searcher();
        BooleanQuery q = new BooleanQuery.Builder()
                .must(new TermQuery("body", "quick"))
                .mustNot(new TermQuery("body", "red"))
                .build();
        assertEquals(Set.of("d0", "d1"), ids(s, q));
    }

    @Test
    void minimumShouldMatchThree() {
        IndexSearcher s = searcher();
        BooleanQuery q = new BooleanQuery.Builder()
                .should(new TermQuery("body", "quick"))
                .should(new TermQuery("body", "brown"))
                .should(new TermQuery("body", "fox"))
                .minimumShouldMatch(3)
                .build();
        assertEquals(Set.of("d0"), ids(s, q)); // only d0 has all three
    }

    @Test
    void phraseRequiresAdjacency() {
        IndexSearcher s = searcher();
        assertEquals(Set.of("d0", "d1"), ids(s, new PhraseQuery("body", List.of("quick", "brown"))));
    }

    @Test
    void phraseRejectsNonAdjacentTerms() {
        IndexSearcher s = searcher();
        assertEquals(Set.of(), ids(s, new PhraseQuery("body", List.of("quick", "fox"))));
    }

    @Test
    void prefixExpandsToMatchingTerms() {
        IndexSearcher s = searcher();
        assertEquals(Set.of("d0", "d1", "d3"), ids(s, new PrefixQuery("body", "qu")));
    }

    @Test
    void requiredPlusOptionalBoostsScoreWithoutChangingMatches() {
        IndexSearcher s = searcher();
        // MUST fox; SHOULD quick only boosts docs that also have quick.
        BooleanQuery q = new BooleanQuery.Builder()
                .must(new TermQuery("body", "fox"))
                .should(new TermQuery("body", "quick"))
                .build();
        TopDocs td = s.search(q, 10);
        assertEquals(3, td.totalHits); // d0, d2, d3 have fox
        // Top hit must be one that also contains "quick" (d0 or d3), never d2.
        assertTrue(Set.of("d0", "d3").contains(s.doc(td.scoreDocs[0].docId).get("id")));
    }
}
