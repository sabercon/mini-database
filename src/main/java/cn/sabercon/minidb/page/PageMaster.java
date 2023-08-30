package cn.sabercon.minidb.page;

import cn.sabercon.minidb.util.Conversions;
import com.google.common.base.Preconditions;

import java.lang.foreign.MemorySegment;

import static cn.sabercon.minidb.page.PageConstants.NULL_POINTER;
import static cn.sabercon.minidb.page.PageConstants.POINTER_SIZE;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

class PageMaster {

    static final MemorySegment SIGNATURE = MemorySegment.ofArray(Conversions.toBytes("MINIDB-SIGNATURE"));

    static final int TOTAL_SIZE = (int) SIGNATURE.byteSize() + 3 * POINTER_SIZE;

    private long total;

    private long root;

    private long freeListHead;

    private PageMaster() {
    }

    static PageMaster empty() {
        var master = new PageMaster();
        master.setTotal(1);
        master.setRoot(NULL_POINTER);
        master.setFreeListHead(NULL_POINTER);
        return master;
    }

    static PageMaster of(MemorySegment data) {
        Preconditions.checkArgument(SIGNATURE.mismatch(data.asSlice(0, SIGNATURE.byteSize())) < 0);

        var master = new PageMaster();
        master.setTotal(data.get(JAVA_LONG, SIGNATURE.byteSize()));
        master.setRoot(data.get(JAVA_LONG, SIGNATURE.byteSize() + POINTER_SIZE));
        master.setFreeListHead(data.get(JAVA_LONG, SIGNATURE.byteSize() + 2 * POINTER_SIZE));
        return master;
    }

    MemorySegment data() {
        var data = MemorySegment.ofArray(new byte[TOTAL_SIZE]);
        data.copyFrom(SIGNATURE);
        data.set(JAVA_LONG, SIGNATURE.byteSize(), total);
        data.set(JAVA_LONG, SIGNATURE.byteSize() + POINTER_SIZE, root);
        data.set(JAVA_LONG, SIGNATURE.byteSize() + 2 * POINTER_SIZE, freeListHead);
        return data;
    }

    long getTotal() {
        return total;
    }

    void setTotal(long total) {
        Preconditions.checkArgument(total >= 1);
        this.total = total;
    }

    long getRoot() {
        return root;
    }

    void setRoot(long root) {
        Preconditions.checkArgument(root >= 0);
        this.root = root;
    }

    long getFreeListHead() {
        return freeListHead;
    }

    void setFreeListHead(long freeListHead) {
        Preconditions.checkArgument(freeListHead >= 0);
        this.freeListHead = freeListHead;
    }
}
