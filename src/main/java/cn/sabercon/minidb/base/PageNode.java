package cn.sabercon.minidb.base;

import java.nio.ByteBuffer;

public abstract class PageNode {

    /**
     * The page size is defined to be 4K bytes.
     */
    public static final int PAGE_SIZE = 4 * 1024;

    protected final ByteBuffer data;

    public ByteBuffer data() {
        return data;
    }

    protected PageNode(ByteBuffer data) {
        this.data = data;
    }

    protected byte[] getBytes(int index, int length) {
        var bytes = new byte[length];
        data.get(index, bytes);
        return bytes;
    }

    protected void putBytes(int index, byte[] bytes) {
        data.put(index, bytes);
    }

    protected short getShort(int index) {
        return data.getShort(index);
    }

    protected void putShort(int index, short value) {
        data.putShort(index, value);
    }

    protected int getInt(int index) {
        return data.getInt(index);
    }

    protected void putInt(int index, int value) {
        data.putInt(index, value);
    }

    protected long getLong(int index) {
        return data.getLong(index);
    }

    protected void putLong(int index, long value) {
        data.putLong(index, value);
    }

    protected void copy(int index, PageNode src, int offset, int length) {
        data.put(index, src.data, offset, length);
    }
}
