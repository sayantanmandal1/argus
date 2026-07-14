package io.argus.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Variable-length integer codec (unsigned LEB128).
 *
 * <p>Small integers occupy fewer bytes, which is the backbone of postings-list compression in the
 * inverted index: document-id gaps and term frequencies are almost always small and therefore
 * encode into one or two bytes instead of a fixed four. This directly reduces on-disk index size
 * and the number of bytes touched during a query scan.
 */
public final class VarInt {

    /** Maximum number of bytes a 32-bit value can occupy when varint-encoded. */
    public static final int MAX_VARINT_SIZE = 5;

    /** Maximum number of bytes a 64-bit value can occupy when varint-encoded. */
    public static final int MAX_VARLONG_SIZE = 10;

    private VarInt() {
    }

    /** Returns the number of bytes {@code value} needs when encoded as an unsigned varint. */
    public static int sizeOf(int value) {
        int bytes = 1;
        int v = value;
        while ((v & ~0x7F) != 0) {
            v >>>= 7;
            bytes++;
        }
        return bytes;
    }

    /** Writes {@code value} to {@code out} as an unsigned varint. */
    public static void writeInt(OutputStream out, int value) throws IOException {
        int v = value;
        while ((v & ~0x7F) != 0) {
            out.write((v & 0x7F) | 0x80);
            v >>>= 7;
        }
        out.write(v);
    }

    /** Reads an unsigned varint written by {@link #writeInt(OutputStream, int)}. */
    public static int readInt(InputStream in) throws IOException {
        int result = 0;
        int shift = 0;
        while (shift < 32) {
            int b = in.read();
            if (b < 0) {
                throw new EOFException("Truncated varint");
            }
            result |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return result;
            }
            shift += 7;
        }
        throw new IOException("Malformed varint: more than " + MAX_VARINT_SIZE + " bytes");
    }

    /** Writes {@code value} to {@code buf} as an unsigned varint. */
    public static void writeInt(ByteBuffer buf, int value) {
        int v = value;
        while ((v & ~0x7F) != 0) {
            buf.put((byte) ((v & 0x7F) | 0x80));
            v >>>= 7;
        }
        buf.put((byte) v);
    }

    /** Reads an unsigned varint from {@code buf}. */
    public static int readInt(ByteBuffer buf) {
        int result = 0;
        int shift = 0;
        while (shift < 32) {
            byte b = buf.get();
            result |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return result;
            }
            shift += 7;
        }
        throw new IllegalStateException("Malformed varint: more than " + MAX_VARINT_SIZE + " bytes");
    }

    /** Writes {@code value} to {@code out} as an unsigned varlong. */
    public static void writeLong(OutputStream out, long value) throws IOException {
        long v = value;
        while ((v & ~0x7FL) != 0) {
            out.write((int) ((v & 0x7F) | 0x80));
            v >>>= 7;
        }
        out.write((int) v);
    }

    /** Reads an unsigned varlong written by {@link #writeLong(OutputStream, long)}. */
    public static long readLong(InputStream in) throws IOException {
        long result = 0;
        int shift = 0;
        while (shift < 64) {
            int b = in.read();
            if (b < 0) {
                throw new EOFException("Truncated varlong");
            }
            result |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return result;
            }
            shift += 7;
        }
        throw new IOException("Malformed varlong: more than " + MAX_VARLONG_SIZE + " bytes");
    }
}
