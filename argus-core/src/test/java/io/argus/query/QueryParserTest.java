package io.argus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.argus.analysis.StandardAnalyzer;
import io.argus.document.Document;
import io.argus.index.IndexWriter;
import io.argus.search.IndexSearcher;
import io.argus.search.ScoreDoc;
import io.argus.search.TopDocs;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

class QueryParserTest {

    private IndexSearcher searcher() {
        IndexWriter w = new IndexWriter();
        w.addDocument(new Document().addKeyword("id", "d0")
                .addText("title", "distributed systems").addText("body", "fault tolerant storage"));
        w.addDocument(new Document().addKeyword("id", "d1")
                .addText("title", "distributed databases").addText("body", "scalable query engine"));
        w.addDocument(new Document().addKeyword("id", "d2")
                .addText("title", "operating systems").addText("body", "fault isolation kernel"));
        return new IndexSearcher(w.getReader());
    }

    private QueryParser parser() {
        return new QueryParser("body", new StandardAnalyzer());
    }

    private Set<String> ids(IndexSearcher s, QueryParser p, String q) {
        TopDocs td = s.search(p.parse(q), 100);
        Set<String> out = new TreeSet<>();
        for (ScoreDoc sd : td.scoreDocs) {
            out.add(s.doc(sd.docId).get("id"));
        }
        return out;
    }

    private Set<String> ids(IndexSearcher s, String q) {
        return ids(s, parser(), q);
    }

    @Test
    void defaultFieldTerm() {
        assertEquals(Set.of("d0", "d2"), ids(searcher(), "fault"));
    }

    @Test
    void fieldQualifiedTerm() {
        assertEquals(Set.of("d0", "d1"), ids(searcher(), "title:distributed"));
        assertEquals(Set.of("d0", "d2"), ids(searcher(), "title:systems"));
    }

    @Test
    void analyzesQueryTermsLikeTheIndex() {
        // "Distributed" (capitalized) must still match the stemmed, lowercased index term.
        assertEquals(Set.of("d0", "d1"), ids(searcher(), "title:Distributed"));
    }

    @Test
    void andRequiresBoth() {
        assertEquals(Set.of("d0"), ids(searcher(), "title:distributed AND body:fault"));
    }

    @Test
    void orUnions() {
        assertEquals(Set.of("d1", "d2"), ids(searcher(), "title:operating OR body:scalable"));
    }

    @Test
    void minusExcludes() {
        assertEquals(Set.of("d0"), ids(searcher(), "fault -title:operating"));
    }

    @Test
    void plusRequires() {
        // +fault (body) required; systems only boosts. d0 and d2 have fault.
        assertEquals(Set.of("d0", "d2"), ids(searcher(), "+fault title:systems"));
    }

    @Test
    void phraseRequiresAdjacency() {
        assertEquals(Set.of("d0"), ids(searcher(), "body:\"fault tolerant\""));
        assertEquals(Set.of(), ids(searcher(), "body:\"fault kernel\""));
    }

    @Test
    void prefixQuery() {
        assertEquals(Set.of("d0", "d1"), ids(searcher(), "title:distr*"));
    }

    @Test
    void groupingWithFieldPushdown() {
        assertEquals(Set.of("d0", "d1", "d2"), ids(searcher(), "title:(distributed OR operating)"));
    }

    @Test
    void defaultOperatorAnd() {
        QueryParser p = parser().defaultOperator(QueryParser.Operator.AND);
        // both terms must be in body: only d2 has fault AND kernel.
        assertEquals(Set.of("d2"), ids(searcher(), p, "fault kernel"));
        // with default OR, the same query would match d0 and d2.
        assertEquals(Set.of("d0", "d2"), ids(searcher(), "fault kernel"));
    }

    @Test
    void emptyQueryMatchesNothing() {
        assertEquals(Set.of(), ids(searcher(), "   "));
    }

    @Test
    void stopWordsInQueryAreDropped() {
        // "the" is a stop word; the query reduces to just "fault".
        assertEquals(Set.of("d0", "d2"), ids(searcher(), "the fault"));
    }

    @Test
    void unterminatedPhraseThrows() {
        QueryParser p = parser();
        assertThrows(QueryParseException.class, () -> p.parse("body:\"fault tolerant"));
    }
}
