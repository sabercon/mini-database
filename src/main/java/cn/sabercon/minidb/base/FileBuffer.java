package cn.sabercon.minidb.base;

import java.lang.foreign.MemorySegment;

public interface FileBuffer {

    static FileBuffer from(String path) {
        return new DefaultFileBuffer(path);
    }

    MemorySegment get(long pointer, long byteSize);

    default MemorySegment get(long pointer) {
        return get(pointer, Page.BYTE_SIZE);
    }

    void set(long pointer, MemorySegment data, long byteSize);

    default void set(long pointer, MemorySegment data) {
        set(pointer, data, Page.BYTE_SIZE);
    }

    void flush();
}
