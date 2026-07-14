package io.argus.store;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DirectoryIOTest {

    private byte[] readAll(Directory dir, String name) {
        try (IndexInput in = dir.openInput(name)) {
            byte[] b = new byte[(int) in.length()];
            in.readBytes(b, 0, b.length);
            return b;
        }
    }

    @Test
    void roundTripsPrimitivesAndStrings() {
        RAMDirectory dir = new RAMDirectory();
        try (IndexOutput o = dir.createOutput("f")) {
            o.writeVInt(300);
            o.writeInt(-5);
            o.writeVLong(1L << 40);
            o.writeLong(-1L);
            o.writeString("héllo wörld \u2603");
            o.writeByte(42);
            o.writeFooter();
        }
        try (IndexInput in = dir.openInput("f")) {
            assertEquals(300, in.readVInt());
            assertEquals(-5, in.readInt());
            assertEquals(1L << 40, in.readVLong());
            assertEquals(-1L, in.readLong());
            assertEquals("héllo wörld \u2603", in.readString());
            assertEquals(42, in.readByte());
        }
    }

    @Test
    void checksumVerifiesCleanFile() {
        RAMDirectory dir = new RAMDirectory();
        try (IndexOutput o = dir.createOutput("f")) {
            o.writeString("data integrity matters");
            o.writeFooter();
        }
        try (IndexInput in = dir.openInput("f")) {
            assertDoesNotThrow(in::verifyChecksum);
        }
    }

    @Test
    void checksumDetectsCorruption() {
        RAMDirectory dir = new RAMDirectory();
        try (IndexOutput o = dir.createOutput("f")) {
            o.writeString("data integrity matters");
            o.writeFooter();
        }
        byte[] bytes = readAll(dir, "f");
        bytes[3] ^= 0xFF; // flip a content byte, leaving the original footer intact
        dir.deleteFile("f");
        try (IndexOutput o = dir.createOutput("f")) {
            o.writeBytes(bytes, 0, bytes.length); // write exact bytes, no new footer
        }
        try (IndexInput in = dir.openInput("f")) {
            assertThrows(CorruptIndexException.class, in::verifyChecksum);
        }
    }

    @Test
    void seekEnablesRandomAccess() {
        RAMDirectory dir = new RAMDirectory();
        try (IndexOutput o = dir.createOutput("f")) {
            o.writeInt(11);
            o.writeInt(22);
            o.writeInt(33);
        }
        try (IndexInput in = dir.openInput("f")) {
            in.seek(4);
            assertEquals(22, in.readInt());
            in.seek(0);
            assertEquals(11, in.readInt());
            in.seek(8);
            assertEquals(33, in.readInt());
        }
    }

    @Test
    void ramDirectoryListsRenamesAndDeletes() {
        RAMDirectory dir = new RAMDirectory();
        try (IndexOutput o = dir.createOutput("a")) {
            o.writeByte(1);
        }
        try (IndexOutput o = dir.createOutput("b")) {
            o.writeByte(2);
        }
        assertArrayEquals(new String[] {"a", "b"}, dir.listAll());
        dir.rename("a", "c");
        assertFalse(dir.fileExists("a"));
        assertTrue(dir.fileExists("c"));
        dir.deleteFile("b");
        assertArrayEquals(new String[] {"c"}, dir.listAll());
    }

    @Test
    void fsDirectoryPersistsAcrossReopen(@TempDir Path tmp) {
        FSDirectory dir = new FSDirectory(tmp);
        try (IndexOutput o = dir.createOutput("seg.data")) {
            o.writeString("persisted");
            o.writeVInt(7);
            o.writeFooter();
        }
        dir.sync(java.util.List.of("seg.data"));

        // Re-open a fresh Directory over the same path.
        FSDirectory reopened = new FSDirectory(tmp);
        assertTrue(reopened.fileExists("seg.data"));
        try (IndexInput in = reopened.openInput("seg.data")) {
            in.verifyChecksum();
            assertEquals("persisted", in.readString());
            assertEquals(7, in.readVInt());
        }
    }
}
