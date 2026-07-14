package io.argus.store;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** An in-memory {@link Directory}. Fast and dependency-free — ideal for tests and ephemeral use. */
public final class RAMDirectory extends Directory {

    private final Map<String, byte[]> files = new ConcurrentHashMap<>();

    @Override
    public IndexOutput createOutput(String name, boolean append) {
        byte[] seed = append ? files.get(name) : null;
        return new RAMOutputStream(name, seed);
    }

    @Override
    public IndexInput openInput(String name) {
        byte[] data = files.get(name);
        if (data == null) {
            throw new CorruptIndexException("no such file: " + name);
        }
        return new ByteArrayIndexInput(data);
    }

    @Override
    public boolean fileExists(String name) {
        return files.containsKey(name);
    }

    @Override
    public void deleteFile(String name) {
        files.remove(name);
    }

    @Override
    public String[] listAll() {
        String[] names = files.keySet().toArray(new String[0]);
        Arrays.sort(names);
        return names;
    }

    @Override
    public void rename(String source, String dest) {
        byte[] data = files.remove(source);
        if (data == null) {
            throw new CorruptIndexException("no such file: " + source);
        }
        files.put(dest, data);
    }

    @Override
    public void sync(Collection<String> names) {
        // memory is already "durable" for the life of the process
    }

    private final class RAMOutputStream extends IndexOutput {
        private byte[] buf;
        private int len;
        private final String name;
        private boolean closed;

        RAMOutputStream(String name, byte[] seed) {
            this.name = name;
            if (seed != null) {
                buf = Arrays.copyOf(seed, Math.max(64, seed.length));
                len = seed.length;
            } else {
                buf = new byte[64];
            }
        }

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
        public void flush() {
            files.put(name, Arrays.copyOf(buf, len));
        }

        @Override
        public void close() {
            if (!closed) {
                closed = true;
                files.put(name, Arrays.copyOf(buf, len));
            }
        }
    }
}
