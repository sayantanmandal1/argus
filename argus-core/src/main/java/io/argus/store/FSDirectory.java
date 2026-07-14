package io.argus.store;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.stream.Stream;

/** A filesystem-backed {@link Directory}. Durable across restarts; {@link #sync} fsyncs to disk. */
public final class FSDirectory extends Directory {

    private final Path root;

    public FSDirectory(Path root) {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        this.root = root;
    }

    @Override
    public IndexOutput createOutput(String name, boolean append) {
        return new FSIndexOutput(root.resolve(name), append);
    }

    @Override
    public IndexInput openInput(String name) {
        try {
            return new ByteArrayIndexInput(Files.readAllBytes(root.resolve(name)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public boolean fileExists(String name) {
        return Files.exists(root.resolve(name));
    }

    @Override
    public void deleteFile(String name) {
        try {
            Files.deleteIfExists(root.resolve(name));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String[] listAll() {
        try (Stream<Path> s = Files.list(root)) {
            return s.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .toArray(String[]::new);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void rename(String source, String dest) {
        try {
            Files.move(root.resolve(source), root.resolve(dest), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void sync(Collection<String> names) {
        for (String name : names) {
            try (FileChannel ch = FileChannel.open(root.resolve(name),
                    StandardOpenOption.READ, StandardOpenOption.WRITE)) {
                ch.force(true);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static final class FSIndexOutput extends IndexOutput {
        private final FileOutputStream fos;
        private final OutputStream out;
        private long pos;

        FSIndexOutput(Path path, boolean append) {
            try {
                fos = new FileOutputStream(path.toFile(), append);
                out = new BufferedOutputStream(fos);
                pos = append ? path.toFile().length() : 0;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        protected void writeByteRaw(int b) {
            try {
                out.write(b);
                pos++;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public long getFilePointer() {
            return pos;
        }

        @Override
        public void flush() {
            try {
                out.flush();
                fos.getFD().sync();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void close() {
            try {
                out.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
