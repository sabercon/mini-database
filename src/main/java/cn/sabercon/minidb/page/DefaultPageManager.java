package cn.sabercon.minidb.page;

import cn.sabercon.minidb.base.FileBuffer;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.lang.foreign.MemorySegment;
import java.util.*;

import static cn.sabercon.minidb.page.PageConstants.NULL_POINTER;
import static cn.sabercon.minidb.page.PageConstants.PAGE_BYTE_SIZE;

class DefaultPageManager implements PageManager {

    private final Map<Long, MemorySegment> updatedPages = new LinkedHashMap<>();

    private final List<Long> freedPages = new ArrayList<>();

    private final List<Long> freePages = new ArrayList<>();

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

        return getUpdatedPage(pointer).orElseGet(() -> getSavedPage(pointer));
    }

    private Optional<MemorySegment> getUpdatedPage(long pointer) {
        return Optional.ofNullable(updatedPages.get(pointer));
    }

    private MemorySegment getSavedPage(long pointer) {
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
            return Optional.of(freePages.remove(freePages.size() - 1));
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
        syncFreedPages();
        syncFreePages();
        syncUpdatedPages();
        buffer.flush();

        syncMaster();
        buffer.flush();
    }

    private void syncFreedPages() {
        if (freedPages.isEmpty()) return;

        for (var freed : Lists.partition(freedPages, FreeListNode.CAPACITY)) {
            var pointer = allocatePage();
            var freedGroup = new ArrayList<>(freed);
            while (freedGroup.size() < FreeListNode.CAPACITY && !freePages.isEmpty()) {
                freedGroup.add(freePages.remove(freePages.size() - 1));
            }
            createFreeListHead(pointer, freedGroup);
        }

        freedPages.clear();
    }

    private void syncFreePages() {
        if (freePages.isEmpty()) return;

        var pointer = freePages.remove(freePages.size() - 1);
        createFreeListHead(pointer, freePages);

        freePages.clear();
    }

    private void createFreeListHead(long pointer, List<Long> freePages) {
        var node = FreeListNode.of(master.getFreeListHead(), freePages);
        updatedPages.put(pointer, node.data());
        master.setFreeListHead(pointer);
    }

    private void syncUpdatedPages() {
        updatedPages.forEach((pointer, page) -> buffer.set(toOffset(pointer), page));
        updatedPages.clear();
    }

    private void syncMaster() {
        buffer.set(0, master.data());
    }

    private static long toOffset(long pointer) {
        return PAGE_BYTE_SIZE * pointer;
    }
}
