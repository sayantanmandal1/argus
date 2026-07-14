package io.argus.analysis;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Common English function words that carry little retrieval value and are filtered by default. */
public final class StopWords {

    private StopWords() {
    }

    /** An immutable default English stop-word set (Lucene-style). */
    public static final Set<String> ENGLISH = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "a", "an", "and", "are", "as", "at", "be", "but", "by",
            "for", "if", "in", "into", "is", "it",
            "no", "not", "of", "on", "or", "such",
            "that", "the", "their", "then", "there", "these",
            "they", "this", "to", "was", "will", "with")));
}
