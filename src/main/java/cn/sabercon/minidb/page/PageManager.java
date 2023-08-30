package cn.sabercon.minidb.page;

import cn.sabercon.minidb.base.FileBuffer;

import java.lang.foreign.MemorySegment;

public interface PageManager {

    static PageManager of(FileBuffer buffer) {
        return new DefaultPageManager(buffer);
    }

    /**
     * @return The point of the root page or zero if the tree is empty
     */
    long getRoot();

    /**
     * @param root The pointer of the root page
     */
    void setRoot(long root);

    /**
     * Dereferences a pointer.
     */
    MemorySegment getPage(long pointer);

    /**
     * Deletes a page.
     *
     * @param pointer The pointer of the page to be deleted
     */
    void deletePage(long pointer);

    /**
     * Allocates a new page.
     *
     * @param page The data to be saved
     * @return The pointer of the created page
     */
    long createPage(MemorySegment page);

    void flush();
}
