package io.argus.search;

/** A scored hit: a document id paired with its relevance score. */
public final class ScoreDoc {

    public final int docId;
    public final double score;

    public ScoreDoc(int docId, double score) {
        this.docId = docId;
        this.score = score;
    }

    @Override
    public String toString() {
        return "ScoreDoc{doc=" + docId + ", score=" + score + "}";
    }
}
