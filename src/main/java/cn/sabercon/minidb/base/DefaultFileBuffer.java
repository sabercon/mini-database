package cn.sabercon.minidb.base;

import com.google.common.base.Preconditions;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentScope;
import java.nio.channels.FileChannel;

class DefaultFileBuffer implements FileBuffer {

    private static final int MIN_BYTE_SIZE = 8 * 1024 * 1024;

    private final String path;

    private MemorySegment buffer;

    DefaultFileBuffer(String path) {
        this.path = path;
        this.buffer = mapFile(path, 0);
    }

    @Override
    public MemorySegment get(long offset, long byteSize) {
        Preconditions.checkArgument(offset + byteSize <= buffer.byteSize());
        return buffer.asSlice(offset, byteSize);
    }

    @Override
    public void set(long offset, MemorySegment data, long byteSize) {
        extendBuffer(offset + byteSize);
        MemorySegment.copy(data, 0, buffer, offset, byteSize);
    }

    @Override
    public long byteSize() {
        return buffer.byteSize();
    }

    @Override
    public void flush() {
        buffer.force();
    }

    private void extendBuffer(long capacity) {
        if (capacity > buffer.byteSize()) {
            var minCap = Math.max(MIN_BYTE_SIZE, capacity);
            buffer = mapFile(path, minCap);
        }
    }

    private static MemorySegment mapFile(String path, long minCap) {
        try (var raf = new RandomAccessFile(path, "rw")) {
            var channel = raf.getChannel();
            var size = bufferSize(Math.max(minCap, channel.size()));
            return channel.map(FileChannel.MapMode.READ_WRITE, 0, size, SegmentScope.auto());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns a power of two size for the given target capacity.
     */
    private static long bufferSize(long cap) {
        if (cap < 2) return cap;
        long n = -1L >>> Long.numberOfLeadingZeros(cap - 1);
        return n + 1;
    }
}
