package cn.sabercon.minidb;

import cn.sabercon.minidb.base.PageStore;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class TestPageStore implements PageStore {

    private final Map<Long, ByteBuffer> pages = new ConcurrentHashMap<>();

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
    public ByteBuffer getData(long pointer) {
        return Objects.requireNonNull(pages.get(pointer));
    }

    @Override
    public long createPage(ByteBuffer data) {
        var pointer = counter.getAndIncrement();
        pages.put(pointer, data);
        return pointer;
    }

    @Override
    public void deletePage(long pointer) {
        pages.remove(pointer);
    }
}
