package io.argus.store;

/** An {@link IndexInput} backed by an in-memory byte array; used for RAM files and loaded segments. */
public final class ByteArrayIndexInput extends IndexInput {

    private final byte[] data;
    private int pos;

    public ByteArrayIndexInput(byte[] data) {
        this.data = data;
    }

    @Override
    public byte readByte() {
        return data[pos++];
    }

    @Override
    public void readBytes(byte[] b, int off, int len) {
        System.arraycopy(data, pos, b, off, len);
        pos += len;
    }

    @Override
    public long getFilePointer() {
        return pos;
    }

    @Override
    public void seek(long p) {
        this.pos = (int) p;
    }

    @Override
    public long length() {
        return data.length;
    }

    @Override
    public void close() {
        // nothing to release
    }
}
