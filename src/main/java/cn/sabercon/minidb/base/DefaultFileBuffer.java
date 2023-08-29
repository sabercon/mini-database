package cn.sabercon.minidb.base;

import com.google.common.base.Preconditions;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentScope;
import java.nio.channels.FileChannel;

public class DefaultFileBuffer implements FileBuffer {

    private static final int MIN_BYTE_SIZE = 8 * 1024 * 1024;

    private final String path;

    private MemorySegment buffer;

    public DefaultFileBuffer(String path) {
        this.path = path;
        this.buffer = mapFile(path, MIN_BYTE_SIZE);
    }

    @Override
    public MemorySegment get(long pointer, long byteSize) {
        var offset = toOffset(pointer);
        Preconditions.checkArgument(offset + byteSize <= buffer.byteSize());
        return buffer.asSlice(offset, byteSize);
    }

    @Override
    public void set(long pointer, MemorySegment data, long byteSize) {
        var offset = toOffset(pointer);
        extendBuffer(offset + byteSize);
        MemorySegment.copy(data, 0, buffer, offset, byteSize);
    }

    @Override
    public void flush() {
        buffer.force();
    }

    private void extendBuffer(long capacity) {
        if (capacity > buffer.byteSize()) {
            var size = bufferSize(capacity);
            buffer = mapFile(path, size);
        }
    }

    private static MemorySegment mapFile(String path, long capacity) {
        try (var raf = new RandomAccessFile(path, "rw")) {
            var channel = raf.getChannel();
            var size = bufferSize(Math.max(capacity, channel.size()));
            return channel.map(FileChannel.MapMode.READ_WRITE, 0, size, SegmentScope.auto());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns a power of two size for the given target capacity.
     */
    private static long bufferSize(long cap) {
        long n = -1L >>> Long.numberOfLeadingZeros(cap - 1);
        return (n < 0) ? 1 : n + 1;
    }

    private static long toOffset(long pointer) {
        return Page.BYTE_SIZE * pointer;
    }
}
