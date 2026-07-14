package io.argus.search;

/** The result of a search: the total number of matches and the top hits in descending score order. */
public final class TopDocs {

    public final long totalHits;
    public final ScoreDoc[] scoreDocs;

    public TopDocs(long totalHits, ScoreDoc[] scoreDocs) {
        this.totalHits = totalHits;
        this.scoreDocs = scoreDocs;
    }

    public int size() {
        return scoreDocs.length;
    }

    @Override
    public String toString() {
        return "TopDocs{totalHits=" + totalHits + ", returned=" + scoreDocs.length + "}";
    }
}
