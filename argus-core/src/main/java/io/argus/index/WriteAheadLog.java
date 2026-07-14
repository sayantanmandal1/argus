package io.argus.index;

import io.argus.store.Directory;
import io.argus.store.IndexInput;
import io.argus.store.IndexOutput;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

/**
 * An append-only write-ahead log. Every mutation is framed as {@code [vint length][bytes][int crc]}
 * and fsynced before the operation is applied in memory, so an unexpected termination can be
 * recovered by replaying the log. Replay stops at the first torn or corrupt record — that is exactly
 * the point at which the process died mid-write — which keeps recovery safe.
 */
public final class WriteAheadLog implements Closeable {

    private final Directory dir;
    private final String name;
    private IndexOutput out;

    private WriteAheadLog(Directory dir, String name, IndexOutput out) {
        this.dir = dir;
        this.name = name;
        this.out = out;
    }

    public static WriteAheadLog open(Directory dir, String name) {
        return new WriteAheadLog(dir, name, dir.createOutput(name, true));
    }

    /** Durably appends one record. */
    public void append(byte[] record) {
        out.writeVInt(record.length);
        out.writeBytes(record, 0, record.length);
        CRC32 crc = new CRC32();
        crc.update(record, 0, record.length);
        out.writeInt((int) crc.getValue());
        out.flush();
    }

    /** Replays intact records in order, stopping at the first incomplete or corrupt one. */
    public static List<byte[]> replay(Directory dir, String name) {
        List<byte[]> records = new ArrayList<>();
        if (!dir.fileExists(name)) {
            return records;
        }
        try (IndexInput in = dir.openInput(name)) {
            long len = in.length();
            while (in.getFilePointer() < len) {
                Integer recordLength = tryReadVInt(in, len);
                if (recordLength == null || in.getFilePointer() + recordLength + 4 > len) {
                    break; // torn tail from a crash mid-append
                }
                byte[] record = new byte[recordLength];
                in.readBytes(record, 0, recordLength);
                int storedCrc = in.readInt();
                CRC32 crc = new CRC32();
                crc.update(record, 0, recordLength);
                if ((int) crc.getValue() != storedCrc) {
                    break; // corrupt tail
                }
                records.add(record);
            }
        }
        return records;
    }

    private static Integer tryReadVInt(IndexInput in, long len) {
        int value = 0;
        int shift = 0;
        while (true) {
            if (in.getFilePointer() >= len) {
                return null;
            }
            int b = in.readByte() & 0xFF;
            value |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return value;
            }
            shift += 7;
            if (shift > 35) {
                return null;
            }
        }
    }

    /** Truncates the log — called after a commit has durably captured everything it held. */
    public void clear() {
        out.close();
        dir.deleteFile(name);
        out = dir.createOutput(name, true);
    }

    @Override
    public void close() {
        out.close();
    }
}
