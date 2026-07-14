package io.argus.store;

import java.io.Closeable;
import java.util.Collection;

/**
 * An abstraction over a flat namespace of named binary files — the storage substrate the index is
 * written to and read from. Implementations include {@link RAMDirectory} (in-memory, for tests and
 * ephemeral indexes) and {@link FSDirectory} (on-disk, durable).
 */
public abstract class Directory implements Closeable {

    /** Creates (or truncates) a file and returns a stream to write it. */
    public final IndexOutput createOutput(String name) {
        return createOutput(name, false);
    }

    /** Opens a file for writing; when {@code append} is true, writes are added to the end. */
    public abstract IndexOutput createOutput(String name, boolean append);

    /** Opens an existing file for reading. */
    public abstract IndexInput openInput(String name);

    public abstract boolean fileExists(String name);

    public abstract void deleteFile(String name);

    public abstract String[] listAll();

    /** Atomically replaces {@code dest} with {@code source}. */
    public abstract void rename(String source, String dest);

    /** Forces the given files' contents to durable storage. */
    public abstract void sync(Collection<String> names);

    @Override
    public void close() {
        // no-op by default
    }
}
