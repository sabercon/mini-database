package cn.sabercon.minidb.page;

import com.google.common.base.Preconditions;

import java.lang.foreign.MemorySegment;
import java.util.List;
import java.util.stream.IntStream;

import static cn.sabercon.minidb.page.PageConstants.*;

class FreeListNode extends Page {

    static final int CAPACITY = (PAGE_BYTE_SIZE - (HEADER_SIZE + POINTER_SIZE)) / POINTER_SIZE;

    private FreeListNode(MemorySegment data) {
        super(data);
    }

    static FreeListNode of(MemorySegment data) {
        return new FreeListNode(data);
    }

    static FreeListNode of(long next, List<Long> freePages) {
        Preconditions.checkState(freePages.size() <= CAPACITY);

        var data = MemorySegment.ofArray(new byte[PAGE_BYTE_SIZE]);
        var node = FreeListNode.of(data);
        node.putInt(0, PageType.FREE_LIST.value());
        node.putInt(NODE_TYPE_SIZE, freePages.size());
        node.putLong(HEADER_SIZE, next);
        for (int i = 0; i < freePages.size(); i++) {
            node.putLong(pointerPos(i), freePages.get(i));
        }
        return node;
    }

    PageType type() {
        return PageType.of(getInt(0));
    }

    int items() {
        return getInt(NODE_TYPE_SIZE);
    }

    long next() {
        return getLong(HEADER_SIZE);
    }

    List<Long> freePages() {
        return IntStream.range(0, items())
                .mapToObj(i -> getLong(pointerPos(i)))
                .toList();
    }

    private static int pointerPos(int index) {
        return HEADER_SIZE + POINTER_SIZE + POINTER_SIZE * index;
    }
}
