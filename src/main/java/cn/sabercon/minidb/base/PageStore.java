package cn.sabercon.minidb.base;

import java.nio.ByteBuffer;

public interface PageStore {

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
    ByteBuffer getData(long pointer);

    /**
     * Allocates a new page.
     *
     * @param data The data to be saved
     * @return The pointer of the created page
     */
    long createPage(ByteBuffer data);

    /**
     * Deletes a page.
     *
     * @param pointer The pointer of the page to be deleted
     */
    void deletePage(long pointer);
}
