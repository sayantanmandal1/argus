package io.argus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class VarIntTest {

    @Test
    void roundTripsIntsThroughStreams() throws IOException {
        int[] values = {0, 1, 127, 128, 300, 16383, 16384, Integer.MAX_VALUE, -1};
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int v : values) {
            VarInt.writeInt(out, v);
        }
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        for (int v : values) {
            assertEquals(v, VarInt.readInt(in));
        }
    }

    @Test
    void roundTripsThroughByteBuffer() {
        int[] values = {0, 5, 127, 128, 65535, 1 << 21, Integer.MAX_VALUE};
        ByteBuffer buf = ByteBuffer.allocate(values.length * VarInt.MAX_VARINT_SIZE);
        for (int v : values) {
            VarInt.writeInt(buf, v);
        }
        buf.flip();
        for (int v : values) {
            assertEquals(v, VarInt.readInt(buf));
        }
    }

    @Test
    void sizeOfMatchesEncodedLength() {
        assertEquals(1, VarInt.sizeOf(0));
        assertEquals(1, VarInt.sizeOf(127));
        assertEquals(2, VarInt.sizeOf(128));
        assertEquals(2, VarInt.sizeOf(16383));
        assertEquals(3, VarInt.sizeOf(16384));
        assertEquals(5, VarInt.sizeOf(-1));
    }

    @Test
    void roundTripsLongs() throws IOException {
        long[] values = {0L, 1L, 1L << 35, Long.MAX_VALUE, -1L};
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (long v : values) {
            VarInt.writeLong(out, v);
        }
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        for (long v : values) {
            assertEquals(v, VarInt.readLong(in));
        }
    }

    @Test
    void throwsOnTruncatedInput() {
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[] {(byte) 0x80});
        assertThrows(EOFException.class, () -> VarInt.readInt(in));
    }
}
