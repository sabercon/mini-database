package cn.sabercon.minidb.base;

import java.lang.foreign.MemorySegment;

public interface PageBuffer {

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
     * Allocates a new page.
     *
     * @param page The data to be saved
     * @return The pointer of the created page
     */
    long createPage(MemorySegment page);

    /**
     * Deletes a page.
     *
     * @param pointer The pointer of the page to be deleted
     */
    void deletePage(long pointer);
}
