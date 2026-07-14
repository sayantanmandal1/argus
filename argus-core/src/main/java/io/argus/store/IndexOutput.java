package io.argus.store;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

/**
 * A binary output stream with the on-disk primitives the index format is built from: fixed and
 * variable-length integers, UTF-8 strings, and a running CRC32 that is written as an 8-byte footer
 * so readers can detect corruption or truncation.
 */
public abstract class IndexOutput implements Closeable {

    private final CRC32 crc = new CRC32();

    /** Writes one raw byte to the underlying medium without touching the checksum. */
    protected abstract void writeByteRaw(int b);

    /** The number of bytes written so far. */
    public abstract long getFilePointer();

    @Override
    public abstract void close();

    /** Flushes buffered bytes to the backing medium (and fsyncs, for durable implementations). */
    public void flush() {
    }

    public final void writeByte(int b) {
        writeByteRaw(b);
        crc.update(b & 0xFF);
    }

    public final void writeBytes(byte[] b, int off, int len) {
        for (int i = 0; i < len; i++) {
            writeByte(b[off + i]);
        }
    }

    public final void writeInt(int v) {
        writeByte((v >>> 24) & 0xFF);
        writeByte((v >>> 16) & 0xFF);
        writeByte((v >>> 8) & 0xFF);
        writeByte(v & 0xFF);
    }

    public final void writeVInt(int v) {
        while ((v & ~0x7F) != 0) {
            writeByte((v & 0x7F) | 0x80);
            v >>>= 7;
        }
        writeByte(v & 0x7F);
    }

    public final void writeLong(long v) {
        writeInt((int) (v >>> 32));
        writeInt((int) (v & 0xFFFFFFFFL));
    }

    public final void writeVLong(long v) {
        while ((v & ~0x7FL) != 0) {
            writeByte((int) ((v & 0x7F) | 0x80));
            v >>>= 7;
        }
        writeByte((int) (v & 0x7F));
    }

    public final void writeString(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        writeVInt(bytes.length);
        writeBytes(bytes, 0, bytes.length);
    }

    public final long checksum() {
        return crc.getValue();
    }

    /** Appends the 8-byte big-endian CRC32 footer (which is itself not covered by the checksum). */
    public final void writeFooter() {
        long c = crc.getValue();
        for (int i = 7; i >= 0; i--) {
            writeByteRaw((int) ((c >>> (i * 8)) & 0xFF));
        }
    }
}
