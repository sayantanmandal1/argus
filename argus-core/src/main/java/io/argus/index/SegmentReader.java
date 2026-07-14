package io.argus.index;

import io.argus.document.Document;
import io.argus.store.Directory;
import io.argus.store.IndexInput;

/**
 * Reads a segment written by {@link SegmentWriter} back into an {@link InMemoryIndex}. Each file's
 * checksum is verified before use. Documents are restored first (which fixes {@code maxDoc}), then
 * field-length statistics and deletes, then the postings.
 */
public final class SegmentReader {

    private SegmentReader() {
    }

    public static InMemoryIndex load(Directory dir, String segment) {
        InMemoryIndex index = new InMemoryIndex();
        loadStoredDocs(dir, segment, index);
        loadMeta(dir, segment, index);
        loadPostings(dir, segment, index);
        return index;
    }

    private static void loadStoredDocs(Directory dir, String segment, InMemoryIndex index) {
        try (IndexInput in = dir.openInput(segment + SegmentWriter.DOCS)) {
            in.verifyChecksum();
            int maxDoc = in.readVInt();
            for (int d = 0; d < maxDoc; d++) {
                Document doc = DocumentCodec.read(in);
                index.newDocId(doc);
            }
        }
    }

    private static void loadMeta(Directory dir, String segment, InMemoryIndex index) {
        try (IndexInput in = dir.openInput(segment + SegmentWriter.META)) {
            in.verifyChecksum();
            int maxDoc = in.readVInt();
            int numFields = in.readVInt();
            for (int f = 0; f < numFields; f++) {
                String field = in.readString();
                for (int d = 0; d < maxDoc; d++) {
                    int length = in.readVInt();
                    if (length > 0) {
                        index.recordFieldLength(d, field, length);
                    }
                }
            }
            int deleted = in.readVInt();
            for (int i = 0; i < deleted; i++) {
                index.markDeleted(in.readVInt());
            }
        }
    }

    private static void loadPostings(Directory dir, String segment, InMemoryIndex index) {
        try (IndexInput in = dir.openInput(segment + SegmentWriter.POST)) {
            in.verifyChecksum();
            int numFields = in.readVInt();
            for (int f = 0; f < numFields; f++) {
                String field = in.readString();
                int numTerms = in.readVInt();
                for (int t = 0; t < numTerms; t++) {
                    String term = in.readString();
                    int docFreq = in.readVInt();
                    int prevDoc = 0;
                    for (int i = 0; i < docFreq; i++) {
                        int docId = prevDoc + in.readVInt();
                        prevDoc = docId;
                        int freq = in.readVInt();
                        int numPositions = in.readVInt();
                        int[] positions = new int[numPositions];
                        int prevPos = 0;
                        for (int p = 0; p < numPositions; p++) {
                            prevPos += in.readVInt();
                            positions[p] = prevPos;
                        }
                        index.addPosting(field, term, new Posting(docId, freq, positions));
                    }
                }
            }
        }
    }
}
