package io.argus.analysis;

import java.util.Objects;
import java.util.Set;

/**
 * Removes tokens whose term is in the configured stop-word set. Dropped tokens' position increments
 * are accumulated onto the next surviving token so that phrase positions remain faithful to the
 * original text.
 */
public final class StopFilter extends TokenFilter {

    private final Set<String> stopWords;

    public StopFilter(TokenStream input, Set<String> stopWords) {
        super(input);
        this.stopWords = Objects.requireNonNull(stopWords, "stopWords");
    }

    @Override
    public boolean incrementToken() {
        int skippedPositions = 0;
        while (input.incrementToken()) {
            Token t = input.token();
            if (!stopWords.contains(t.term())) {
                if (skippedPositions != 0) {
                    t.setPositionIncrement(t.positionIncrement() + skippedPositions);
                }
                return true;
            }
            skippedPositions += t.positionIncrement();
        }
        return false;
    }
}
