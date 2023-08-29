package cn.sabercon.minidb.btree;

import cn.sabercon.minidb.base.FileBuffer;
import cn.sabercon.minidb.base.Page;
import cn.sabercon.minidb.base.PageBuffer;
import com.google.common.base.Preconditions;

import java.lang.foreign.MemorySegment;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

class BTreeBuffer implements PageBuffer {

    private final Map<Long, MemorySegment> tempPages = new LinkedHashMap<>();

    private final FileBuffer buffer;

    private BTreeMaster master;

    BTreeBuffer(FileBuffer buffer) {
        this.buffer = buffer;
        this.master = buffer.byteSize() == 0 ? new BTreeMaster(0, 1)
                : BTreeMaster.from(buffer.get(0, BTreeMaster.TOTAL_SIZE));
    }

    @Override
    public long getRoot() {
        return master.root();
    }

    @Override
    public void setRoot(long root) {
        var flushed = master.flushed() + tempPages.size();
        flush();

        master = new BTreeMaster(root, flushed);
        buffer.set(0, master.data());
        flush();
    }

    private void flush() {
        tempPages.forEach(buffer::set);
        tempPages.clear();
        buffer.flush();
    }

    @Override
    public MemorySegment getPage(long pointer) {
        Objects.checkIndex(pointer, master.flushed() + tempPages.size());

        return pointer >= master.flushed() ? tempPages.get(pointer) : buffer.get(pointer, Page.BYTE_SIZE);
    }

    @Override
    public long createPage(MemorySegment page) {
        Preconditions.checkArgument(page.byteSize() == Page.BYTE_SIZE);

        var pointer = nextPointer();
        tempPages.put(pointer, page);
        return pointer;
    }

    @Override
    public void deletePage(long pointer) {

    }

    private long nextPointer() {
        return master.flushed() + tempPages.size();
    }
}
