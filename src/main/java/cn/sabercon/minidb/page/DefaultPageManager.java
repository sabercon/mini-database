package cn.sabercon.minidb.page;

import cn.sabercon.minidb.base.FileBuffer;
import com.google.common.base.Preconditions;

import java.lang.foreign.MemorySegment;
import java.util.*;

import static cn.sabercon.minidb.page.PageConstants.NULL_POINTER;
import static cn.sabercon.minidb.page.PageConstants.PAGE_BYTE_SIZE;

class DefaultPageManager implements PageManager {

    private final Map<Long, MemorySegment> updatedPages = new LinkedHashMap<>();

    private final Queue<Long> freedPages = new LinkedList<>();

    private final Queue<Long> freePages = new LinkedList<>();

    private final FileBuffer buffer;

    private final PageMaster master;

    DefaultPageManager(FileBuffer buffer) {
        this.buffer = buffer;
        this.master = buffer.byteSize() == 0 ? PageMaster.empty() : PageMaster.of(buffer.get(0, PAGE_BYTE_SIZE));
    }

    @Override
    public long getRoot() {
        return master.getRoot();
    }

    @Override
    public void setRoot(long root) {
        master.setRoot(root);
    }

    @Override
    public MemorySegment getPage(long pointer) {
        Preconditions.checkArgument(pointer > 0 && pointer < master.getTotal());

        return getUpdatedPage(pointer).orElseGet(() -> getSyncedPage(pointer));
    }

    private Optional<MemorySegment> getUpdatedPage(long pointer) {
        return Optional.ofNullable(updatedPages.get(pointer));
    }

    private MemorySegment getSyncedPage(long pointer) {
        return buffer.get(toOffset(pointer), PAGE_BYTE_SIZE);
    }

    @Override
    public void deletePage(long pointer) {
        Preconditions.checkArgument(pointer > 0 && pointer < master.getTotal());

        freedPages.add(pointer);
    }

    @Override
    public long createPage(MemorySegment page) {
        Preconditions.checkArgument(page.byteSize() == PAGE_BYTE_SIZE);

        var pointer = allocatePage();
        updatedPages.put(pointer, page);
        return pointer;
    }

    private long allocatePage() {
        return allocateFreePage().orElseGet(this::allocateNewPage);
    }

    private Optional<Long> allocateFreePage() {
        if (!freePages.isEmpty()) {
            return Optional.of(freePages.remove());
        }
        var freeListHead = master.getFreeListHead();
        if (freeListHead == NULL_POINTER) {
            return Optional.empty();
        }
        // When the free list head is empty, free pages need to be updated twice.
        updateFreePages(freeListHead);
        return allocateFreePage();
    }

    private void updateFreePages(long freeListHead) {
        var node = FreeListNode.of(getPage(freeListHead));
        deletePage(freeListHead);

        freePages.addAll(node.freePages());
        assert freePages.size() <= FreeListNode.CAPACITY;
        master.setFreeListHead(node.next());
    }

    private long allocateNewPage() {
        var pointer = master.getTotal();
        master.setTotal(pointer + 1);
        return pointer;
    }

    @Override
    public void flush() {
        syncFreeList();
        syncUpdatedPages();
        buffer.flush();

        syncMaster();
        buffer.flush();
    }

    private void syncFreeList() {
        if (freedPages.isEmpty() && freePages.isEmpty()) return;

        var pointer = allocatePage();
        var freeablePages = findFreeablePages();
        var node = FreeListNode.of(master.getFreeListHead(), freeablePages);
        updatedPages.put(pointer, node.data());
        master.setFreeListHead(pointer);

        syncFreeList();
    }

    private List<Long> findFreeablePages() {
        var size = Math.min(FreeListNode.CAPACITY, freedPages.size() + freePages.size());
        var freeablePages = new ArrayList<Long>(size);

        while (freeablePages.size() < size) {
            if (!freedPages.isEmpty()) {
                freeablePages.add(freedPages.remove());
            } else {
                freeablePages.add(freePages.remove());
            }
        }
        return freeablePages;
    }

    private void syncUpdatedPages() {
        var updatablePages = Map.copyOf(updatedPages);
        updatedPages.clear();
        updatablePages.forEach((pointer, page) -> buffer.set(toOffset(pointer), page));
    }

    private void syncMaster() {
        buffer.set(0, master.data());
    }

    private static long toOffset(long pointer) {
        return PAGE_BYTE_SIZE * pointer;
    }
}
