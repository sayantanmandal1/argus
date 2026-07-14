package io.argus.search;

import io.argus.index.IndexReader;
import io.argus.index.PostingList;
import java.util.ArrayList;
import java.util.List;

/**
 * Matches documents where a sequence of terms occurs consecutively (an exact phrase). Candidate
 * documents come from a conjunction of the terms' postings; each candidate is then verified by a
 * k-way merge over the terms' position lists, offsetting term {@code i} by {@code i} so that a
 * common value marks a phrase occurrence. The number of occurrences becomes the phrase frequency
 * fed to the {@link Similarity}.
 */
public final class PhraseScorer extends Scorer {

    private final IndexReader reader;
    private final String field;
    private final TermScorer[] termScorers;
    private final ConjunctionScorer conjunction;
    private final Similarity similarity;
    private final double idfSum;
    private final double avgFieldLength;
    private final int[][] positions;

    private int docId = -1;
    private int phraseFreq;

    public PhraseScorer(IndexReader reader, String field, List<PostingList> lists, Similarity similarity) {
        this.reader = reader;
        this.field = field;
        this.similarity = similarity;
        this.termScorers = new TermScorer[lists.size()];
        List<Scorer> asScorers = new ArrayList<>(lists.size());
        double idf = 0;
        for (int i = 0; i < lists.size(); i++) {
            termScorers[i] = new TermScorer(reader, field, lists.get(i), similarity);
            asScorers.add(termScorers[i]);
            idf += termScorers[i].idf();
        }
        this.idfSum = idf;
        this.conjunction = new ConjunctionScorer(asScorers);
        this.avgFieldLength = reader.averageFieldLength(field);
        this.positions = new int[lists.size()][];
    }

    @Override
    public int docId() {
        return docId;
    }

    @Override
    public int nextDoc() {
        return verify(conjunction.nextDoc());
    }

    @Override
    public int advance(int target) {
        return verify(conjunction.advance(target));
    }

    private int verify(int candidate) {
        while (candidate != NO_MORE_DOCS) {
            for (int i = 0; i < termScorers.length; i++) {
                positions[i] = termScorers[i].positions();
            }
            phraseFreq = countPhrase();
            if (phraseFreq > 0) {
                docId = candidate;
                return docId;
            }
            candidate = conjunction.nextDoc();
        }
        docId = NO_MORE_DOCS;
        return NO_MORE_DOCS;
    }

    private int countPhrase() {
        int n = positions.length;
        int[] ptr = new int[n];
        int freq = 0;
        while (true) {
            int maxNorm = Integer.MIN_VALUE;
            for (int i = 0; i < n; i++) {
                if (ptr[i] >= positions[i].length) {
                    return freq;
                }
                int norm = positions[i][ptr[i]] - i;
                if (norm > maxNorm) {
                    maxNorm = norm;
                }
            }
            boolean allEqual = true;
            for (int i = 0; i < n; i++) {
                int norm = positions[i][ptr[i]] - i;
                if (norm < maxNorm) {
                    allEqual = false;
                    do {
                        ptr[i]++;
                    } while (ptr[i] < positions[i].length && positions[i][ptr[i]] - i < maxNorm);
                }
            }
            if (allEqual) {
                freq++;
                for (int i = 0; i < n; i++) {
                    ptr[i]++;
                }
            }
        }
    }

    @Override
    public double score() {
        int fieldLength = reader.fieldLength(docId, field);
        return similarity.score(phraseFreq, idfSum, fieldLength, avgFieldLength);
    }

    @Override
    public long cost() {
        return conjunction.cost();
    }
}
