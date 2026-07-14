package io.argus.util;

import java.util.Arrays;

/**
 * A growable list of primitive {@code int}s — avoids the boxing overhead of {@code List<Integer>}
 * on the hot indexing path where we accumulate term positions per document.
 */
public final class IntArrayList {

    private int[] a;
    private int size;

    public IntArrayList() {
        this(16);
    }

    public IntArrayList(int initialCapacity) {
        a = new int[Math.max(1, initialCapacity)];
    }

    public void add(int value) {
        if (size == a.length) {
            grow(size + 1);
        }
        a[size++] = value;
    }

    public int get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index " + index + ", size " + size);
        }
        return a[index];
    }

    /** Sets the value at {@code index}, growing (and zero-filling any gap) if necessary. */
    public void set(int index, int value) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("index " + index);
        }
        if (index >= size) {
            if (index >= a.length) {
                grow(index + 1);
            }
            for (int i = size; i < index; i++) {
                a[i] = 0;
            }
            size = index + 1;
        }
        a[index] = value;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        size = 0;
    }

    public int[] toArray() {
        return Arrays.copyOf(a, size);
    }

    private void grow(int minCapacity) {
        int newCap = a.length + (a.length >> 1) + 1;
        if (newCap < minCapacity) {
            newCap = minCapacity;
        }
        a = Arrays.copyOf(a, newCap);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(a[i]);
        }
        return sb.append(']').toString();
    }
}
