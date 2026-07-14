package io.argus.index;

import io.argus.document.Document;
import io.argus.store.Directory;
import io.argus.store.IndexOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Writes a point-in-time snapshot of an {@link IndexReader} to an immutable on-disk segment, made of
 * three files:
 * <ul>
 *   <li>{@code <seg>.docs} — the stored documents, in document-id order</li>
 *   <li>{@code <seg>.meta} — per-field, per-document lengths and the set of deleted documents</li>
 *   <li>{@code <seg>.post} — the term dictionary and postings, with delta-and-varint-compressed
 *       document ids and positions</li>
 * </ul>
 * Each file ends with a CRC32 footer so the reader can detect corruption.
 */
public final class SegmentWriter {

    public static final String DOCS = ".docs";
    public static final String META = ".meta";
    public static final String POST = ".post";

    private SegmentWriter() {
    }

    public static void write(Directory dir, String segment, IndexReader reader) {
        writeStoredDocs(dir, segment, reader);
        writeMeta(dir, segment, reader);
        writePostings(dir, segment, reader);
    }

    private static void writeStoredDocs(Directory dir, String segment, IndexReader reader) {
        try (IndexOutput out = dir.createOutput(segment + DOCS)) {
            int maxDoc = reader.maxDoc();
            out.writeVInt(maxDoc);
            for (int d = 0; d < maxDoc; d++) {
                Document doc = reader.document(d);
                DocumentCodec.write(out, doc == null ? new Document() : doc);
            }
            out.writeFooter();
        }
    }

    private static void writeMeta(Directory dir, String segment, IndexReader reader) {
        List<String> fields = sorted(reader.fields());
        try (IndexOutput out = dir.createOutput(segment + META)) {
            int maxDoc = reader.maxDoc();
            out.writeVInt(maxDoc);
            out.writeVInt(fields.size());
            for (String field : fields) {
                out.writeString(field);
                for (int d = 0; d < maxDoc; d++) {
                    out.writeVInt(reader.fieldLength(d, field));
                }
            }
            int deleted = 0;
            for (int d = 0; d < maxDoc; d++) {
                if (reader.isDeleted(d)) {
                    deleted++;
                }
            }
            out.writeVInt(deleted);
            for (int d = 0; d < maxDoc; d++) {
                if (reader.isDeleted(d)) {
                    out.writeVInt(d);
                }
            }
            out.writeFooter();
        }
    }

    private static void writePostings(Directory dir, String segment, IndexReader reader) {
        List<String> fields = sorted(reader.fields());
        try (IndexOutput out = dir.createOutput(segment + POST)) {
            out.writeVInt(fields.size());
            for (String field : fields) {
                out.writeString(field);
                List<String> terms = sorted(reader.terms(field));
                out.writeVInt(terms.size());
                for (String term : terms) {
                    out.writeString(term);
                    List<Posting> postings = reader.postings(field, term).postings();
                    out.writeVInt(postings.size());
                    int prevDoc = 0;
                    for (Posting p : postings) {
                        out.writeVInt(p.docId() - prevDoc);
                        prevDoc = p.docId();
                        out.writeVInt(p.frequency());
                        int[] positions = p.positions();
                        out.writeVInt(positions.length);
                        int prevPos = 0;
                        for (int position : positions) {
                            out.writeVInt(position - prevPos);
                            prevPos = position;
                        }
                    }
                }
            }
            out.writeFooter();
        }
    }

    private static List<String> sorted(Collection<String> c) {
        List<String> list = new ArrayList<>(c);
        Collections.sort(list);
        return list;
    }
}
