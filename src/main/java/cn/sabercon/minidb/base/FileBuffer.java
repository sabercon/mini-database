package cn.sabercon.minidb.base;

import java.lang.foreign.MemorySegment;
import java.nio.file.Path;

public interface FileBuffer {

    static FileBuffer from(Path path) {
        return new DefaultFileBuffer(path);
    }

    MemorySegment get(long offset, long byteSize);

    void set(long pointer, MemorySegment data, long byteSize);

    default void set(long offset, MemorySegment data) {
        set(offset, data, data.byteSize());
    }

    long byteSize();

    void flush();
}
