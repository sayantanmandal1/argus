package io.argus.index;

import io.argus.store.Directory;
import io.argus.store.IndexInput;
import io.argus.store.IndexOutput;
import java.util.List;

/**
 * The commit point of the index: a tiny file naming the current segment and a monotonically
 * increasing generation. It is updated by writing a temp file, fsyncing, and atomically renaming it
 * into place, so a reader always sees either the old manifest or the new one — never a partial write.
 */
public final class Manifest {

    static final String FILE = "segments";

    private final int generation;
    private final String segment;

    Manifest(int generation, String segment) {
        this.generation = generation;
        this.segment = segment;
    }

    public int generation() {
        return generation;
    }

    public String segment() {
        return segment;
    }

    public boolean hasSegment() {
        return segment != null;
    }

    public static Manifest read(Directory dir) {
        if (!dir.fileExists(FILE)) {
            return new Manifest(0, null);
        }
        try (IndexInput in = dir.openInput(FILE)) {
            in.verifyChecksum();
            int generation = in.readVInt();
            boolean has = in.readByte() != 0;
            String segment = has ? in.readString() : null;
            return new Manifest(generation, segment);
        }
    }

    public static void write(Directory dir, int generation, String segment) {
        String tmp = FILE + ".tmp";
        try (IndexOutput out = dir.createOutput(tmp)) {
            out.writeVInt(generation);
            out.writeByte(segment != null ? 1 : 0);
            if (segment != null) {
                out.writeString(segment);
            }
            out.writeFooter();
        }
        dir.sync(List.of(tmp));
        dir.rename(tmp, FILE);
        dir.sync(List.of(FILE));
    }
}
