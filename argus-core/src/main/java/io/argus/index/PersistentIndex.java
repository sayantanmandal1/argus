package io.argus.index;

import io.argus.analysis.Analyzer;
import io.argus.analysis.StandardAnalyzer;
import io.argus.document.Document;
import io.argus.search.IndexSearcher;
import io.argus.store.ByteArrayIndexInput;
import io.argus.store.ByteArrayIndexOutput;
import io.argus.store.Directory;
import java.io.Closeable;
import java.util.List;

/**
 * A durable, crash-safe index over a {@link Directory}. Documents are held in a live in-memory index
 * for fast search; every mutation is first appended to a {@link WriteAheadLog}. {@link #commit()}
 * snapshots the live index to an immutable segment, swaps the {@link Manifest} atomically, and
 * truncates the log. On {@link #open}, the latest committed segment is loaded and any log records
 * written since that commit are replayed — so an add-then-crash loses nothing.
 *
 * <p>This is the single-segment "snapshot on commit" model; incremental multi-segment merging is a
 * separate layer built on the same primitives.
 */
public final class PersistentIndex implements Closeable {

    private static final byte OP_ADD = 1;
    private static final byte OP_DELETE = 2;
    private static final String WAL = "wal.log";

    private final Directory dir;
    private final InMemoryIndex live;
    private final IndexWriter writer;
    private final WriteAheadLog wal;
    private int generation;

    private PersistentIndex(Directory dir, InMemoryIndex live, IndexWriter writer,
                            WriteAheadLog wal, int generation) {
        this.dir = dir;
        this.live = live;
        this.writer = writer;
        this.wal = wal;
        this.generation = generation;
    }

    public static PersistentIndex open(Directory dir) {
        return open(dir, new StandardAnalyzer());
    }

    public static PersistentIndex open(Directory dir, Analyzer analyzer) {
        Manifest manifest = Manifest.read(dir);
        InMemoryIndex live = manifest.hasSegment()
                ? SegmentReader.load(dir, manifest.segment())
                : new InMemoryIndex();
        IndexWriter writer = new IndexWriter(analyzer, IndexWriter.DEFAULT_POSITION_INCREMENT_GAP, live);
        for (byte[] record : WriteAheadLog.replay(dir, WAL)) {
            apply(writer, record);
        }
        WriteAheadLog wal = WriteAheadLog.open(dir, WAL);
        return new PersistentIndex(dir, live, writer, wal, manifest.generation());
    }

    public synchronized int addDocument(Document doc) {
        wal.append(encodeAdd(doc));
        return writer.addDocument(doc);
    }

    public synchronized int deleteByTerm(String field, String term) {
        wal.append(encodeDelete(field, term));
        return writer.deleteByTerm(field, term);
    }

    /** Durably persists everything currently in the index and truncates the write-ahead log. */
    public synchronized void commit() {
        int next = generation + 1;
        String segment = "seg_" + next;
        SegmentWriter.write(dir, segment, live);
        dir.sync(List.of(segment + SegmentWriter.DOCS,
                segment + SegmentWriter.META, segment + SegmentWriter.POST));
        String previous = generation > 0 ? "seg_" + generation : null;
        Manifest.write(dir, next, segment);
        generation = next;
        wal.clear();
        if (previous != null) {
            deleteSegmentFiles(previous);
        }
    }

    public IndexReader getReader() {
        return live;
    }

    public IndexSearcher searcher() {
        return new IndexSearcher(live);
    }

    public int numDocs() {
        return live.numDocs();
    }

    public int maxDoc() {
        return live.maxDoc();
    }

    public int generation() {
        return generation;
    }

    @Override
    public synchronized void close() {
        wal.close();
        dir.close();
    }

    private static byte[] encodeAdd(Document doc) {
        ByteArrayIndexOutput out = new ByteArrayIndexOutput();
        out.writeByte(OP_ADD);
        DocumentCodec.write(out, doc);
        return out.toByteArray();
    }

    private static byte[] encodeDelete(String field, String term) {
        ByteArrayIndexOutput out = new ByteArrayIndexOutput();
        out.writeByte(OP_DELETE);
        out.writeString(field);
        out.writeString(term);
        return out.toByteArray();
    }

    private static void apply(IndexWriter writer, byte[] record) {
        ByteArrayIndexInput in = new ByteArrayIndexInput(record);
        byte op = in.readByte();
        if (op == OP_ADD) {
            writer.addDocument(DocumentCodec.read(in));
        } else if (op == OP_DELETE) {
            String field = in.readString();
            String term = in.readString();
            writer.deleteByTerm(field, term);
        }
    }

    private void deleteSegmentFiles(String segment) {
        for (String suffix : new String[] {SegmentWriter.DOCS, SegmentWriter.META, SegmentWriter.POST}) {
            if (dir.fileExists(segment + suffix)) {
                dir.deleteFile(segment + suffix);
            }
        }
    }
}
