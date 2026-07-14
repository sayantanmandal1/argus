package io.argus.analysis;

/**
 * Emits the entire input as a single token (after optional lowercasing). Used for keyword fields
 * such as ids, tags, and enum values where the value must match exactly.
 */
public final class KeywordAnalyzer extends Analyzer {

    private final boolean lowercase;

    public KeywordAnalyzer() {
        this(false);
    }

    public KeywordAnalyzer(boolean lowercase) {
        this.lowercase = lowercase;
    }

    @Override
    protected TokenStream createTokenStream(String field, String text) {
        return new WholeStringTokenizer(text, lowercase);
    }

    private static final class WholeStringTokenizer extends Tokenizer {
        private final String text;
        private final boolean lowercase;
        private boolean emitted;

        WholeStringTokenizer(String text, boolean lowercase) {
            this.text = text == null ? "" : text;
            this.lowercase = lowercase;
        }

        @Override
        public boolean incrementToken() {
            if (emitted || text.isEmpty()) {
                return false;
            }
            emitted = true;
            token.clear();
            token.setTerm(lowercase ? text.toLowerCase(java.util.Locale.ROOT) : text);
            token.setOffsets(0, text.length());
            token.setPositionIncrement(1);
            return true;
        }

        @Override
        public void reset() {
            emitted = false;
        }
    }
}
