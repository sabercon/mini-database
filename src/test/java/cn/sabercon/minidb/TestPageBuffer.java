package cn.sabercon.minidb;

import cn.sabercon.minidb.base.PageBuffer;

import java.lang.foreign.MemorySegment;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class TestPageBuffer implements PageBuffer {

    private final Map<Long, MemorySegment> pages = new ConcurrentHashMap<>();

    private final AtomicLong counter = new AtomicLong(1);

    private long root = 0;

    @Override
    public long getRoot() {
        return root;
    }

    @Override
    public void setRoot(long root) {
        this.root = root;
    }

    @Override
    public MemorySegment getPage(long pointer) {
        return Objects.requireNonNull(pages.get(pointer));
    }

    @Override
    public long createPage(MemorySegment page) {
        var pointer = counter.getAndIncrement();
        pages.put(pointer, page);
        return pointer;
    }

    @Override
    public void deletePage(long pointer) {
        pages.remove(pointer);
    }
}
