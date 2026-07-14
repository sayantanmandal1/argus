package io.argus.analysis;

import java.util.Objects;
import java.util.Set;

/**
 * The default text analyzer: {@link StandardTokenizer} -> {@link LowerCaseFilter} ->
 * {@link StopFilter} -> {@link PorterStemFilter}. Suitable for English free text.
 */
public final class StandardAnalyzer extends Analyzer {

    private final Set<String> stopWords;
    private final boolean stem;

    public StandardAnalyzer() {
        this(StopWords.ENGLISH, true);
    }

    public StandardAnalyzer(Set<String> stopWords, boolean stem) {
        this.stopWords = Objects.requireNonNull(stopWords, "stopWords");
        this.stem = stem;
    }

    @Override
    protected TokenStream createTokenStream(String field, String text) {
        TokenStream ts = new LowerCaseFilter(new StandardTokenizer(text));
        ts = new StopFilter(ts, stopWords);
        if (stem) {
            ts = new PorterStemFilter(ts);
        }
        return ts;
    }
}
