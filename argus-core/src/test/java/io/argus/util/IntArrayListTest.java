package io.argus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IntArrayListTest {

    @Test
    void addsAndGrows() {
        IntArrayList list = new IntArrayList(2);
        for (int i = 0; i < 100; i++) {
            list.add(i * 2);
        }
        assertEquals(100, list.size());
        assertEquals(0, list.get(0));
        assertEquals(198, list.get(99));
    }

    @Test
    void setPastEndZeroFillsGap() {
        IntArrayList list = new IntArrayList();
        list.set(5, 42);
        assertEquals(6, list.size());
        assertEquals(0, list.get(0));
        assertEquals(0, list.get(4));
        assertEquals(42, list.get(5));
    }

    @Test
    void toArrayReturnsExactContents() {
        IntArrayList list = new IntArrayList();
        list.add(3);
        list.add(1);
        list.add(4);
        assertArrayEquals(new int[] {3, 1, 4}, list.toArray());
    }

    @Test
    void boundsAreChecked() {
        IntArrayList list = new IntArrayList();
        list.add(1);
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
    }

    @Test
    void clearResetsSize() {
        IntArrayList list = new IntArrayList();
        list.add(9);
        list.clear();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }
}
