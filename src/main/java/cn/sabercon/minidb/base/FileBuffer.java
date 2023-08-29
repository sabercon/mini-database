package cn.sabercon.minidb.base;

import java.lang.foreign.MemorySegment;

public interface FileBuffer {

    static FileBuffer from(String path) {
        return new DefaultFileBuffer(path);
    }

    long byteSize();

    MemorySegment get(long pointer, long byteSize);

    void set(long pointer, MemorySegment data, long byteSize);

    default void set(long pointer, MemorySegment data) {
        set(pointer, data, data.byteSize());
    }

    void flush();
}
