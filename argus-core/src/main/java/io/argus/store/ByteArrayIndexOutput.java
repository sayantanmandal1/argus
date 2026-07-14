package io.argus.store;

import java.util.Arrays;

/** An {@link IndexOutput} that accumulates into a byte array — handy for building small records. */
public final class ByteArrayIndexOutput extends IndexOutput {

    private byte[] buf = new byte[64];
    private int len;

    @Override
    protected void writeByteRaw(int b) {
        if (len == buf.length) {
            buf = Arrays.copyOf(buf, buf.length + (buf.length >> 1) + 1);
        }
        buf[len++] = (byte) b;
    }

    @Override
    public long getFilePointer() {
        return len;
    }

    @Override
    public void close() {
        // nothing to release
    }

    public byte[] toByteArray() {
        return Arrays.copyOf(buf, len);
    }
}
