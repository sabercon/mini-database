package cn.sabercon.minidb.page;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.ValueLayout.*;

public abstract class Page {

    protected final MemorySegment data;

    protected Page(MemorySegment data) {
        this.data = data;
    }

    public MemorySegment data() {
        return data;
    }

    protected byte[] getBytes(int index, int length) {
        var bytes = new byte[length];
        MemorySegment.copy(data, JAVA_BYTE, index, bytes, 0, length);
        return bytes;
    }

    protected void putBytes(int index, byte[] bytes) {
        MemorySegment.copy(bytes, 0, data, JAVA_BYTE, index, bytes.length);
    }

    protected short getShort(int index) {
        return data.get(JAVA_SHORT_UNALIGNED, index);
    }

    protected void putShort(int index, short value) {
        data.set(JAVA_SHORT_UNALIGNED, index, value);
    }

    protected int getInt(int index) {
        return data.get(JAVA_INT_UNALIGNED, index);
    }

    protected void putInt(int index, int value) {
        data.set(JAVA_INT_UNALIGNED, index, value);
    }

    protected long getLong(int index) {
        return data.get(JAVA_LONG_UNALIGNED, index);
    }

    protected void putLong(int index, long value) {
        data.set(JAVA_LONG_UNALIGNED, index, value);
    }

    protected void copy(int index, Page src, int offset, int length) {
        MemorySegment.copy(src.data, offset, data, index, length);
    }
}
