package cn.sabercon.minidb.page;

public final class PageConstants {

    private PageConstants() {
        throw new UnsupportedOperationException();
    }

    /**
     * The page size is defined to be 4K bytes.
     */
    public static final int PAGE_BYTE_SIZE = 4 * 1024;

    public static final int NODE_TYPE_SIZE = Integer.BYTES;
    public static final int ITEM_NUMBER_SIZE = Integer.BYTES;
    /**
     * The fixed-sized header containing the type of the node and the number of items.
     */
    public static final int HEADER_SIZE = NODE_TYPE_SIZE + ITEM_NUMBER_SIZE;

    public static final int POINTER_SIZE = Long.BYTES;

    public static final long NULL_POINTER = 0;
}
