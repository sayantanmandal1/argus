package io.argus.store;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

/**
 * A seekable binary input stream mirroring {@link IndexOutput}. Reads the same primitives and can
 * verify the CRC32 footer written by {@link IndexOutput#writeFooter()}.
 */
public abstract class IndexInput implements Closeable {

    public abstract byte readByte();

    public abstract void readBytes(byte[] b, int off, int len);

    public abstract long getFilePointer();

    public abstract void seek(long pos);

    public abstract long length();

    @Override
    public abstract void close();

    public final int readInt() {
        return ((readByte() & 0xFF) << 24)
                | ((readByte() & 0xFF) << 16)
                | ((readByte() & 0xFF) << 8)
                | (readByte() & 0xFF);
    }

    public final int readVInt() {
        int b = readByte();
        int v = b & 0x7F;
        int shift = 7;
        while ((b & 0x80) != 0) {
            b = readByte();
            v |= (b & 0x7F) << shift;
            shift += 7;
        }
        return v;
    }

    public final long readLong() {
        return ((long) readInt() << 32) | (readInt() & 0xFFFFFFFFL);
    }

    public final long readVLong() {
        int b = readByte();
        long v = b & 0x7F;
        int shift = 7;
        while ((b & 0x80) != 0) {
            b = readByte();
            v |= (long) (b & 0x7F) << shift;
            shift += 7;
        }
        return v;
    }

    public final String readString() {
        int len = readVInt();
        byte[] bytes = new byte[len];
        readBytes(bytes, 0, len);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /** Recomputes the CRC32 over the content and compares it to the stored footer. */
    public final void verifyChecksum() {
        long len = length();
        if (len < 8) {
            throw new CorruptIndexException("file too short to contain a checksum footer");
        }
        long saved = getFilePointer();
        seek(0);
        CRC32 crc = new CRC32();
        for (long i = 0; i < len - 8; i++) {
            crc.update(readByte() & 0xFF);
        }
        long stored = 0;
        for (int i = 0; i < 8; i++) {
            stored = (stored << 8) | (readByte() & 0xFF);
        }
        seek(saved);
        if (crc.getValue() != stored) {
            throw new CorruptIndexException(
                    "checksum mismatch: computed " + crc.getValue() + " but stored " + stored);
        }
    }
}
