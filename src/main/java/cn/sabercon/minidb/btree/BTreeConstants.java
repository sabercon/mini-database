package cn.sabercon.minidb.btree;

final class BTreeConstants {

    private BTreeConstants() {
        throw new UnsupportedOperationException();
    }

    static final int NODE_TYPE_SIZE = Integer.BYTES;
    static final int KEYS_NUMBER_SIZE = Integer.BYTES;
    /**
     * The fixed-sized header containing the type of the node and the number of keys.
     */
    static final int HEADER_SIZE = NODE_TYPE_SIZE + KEYS_NUMBER_SIZE;

    static final int OFFSET_SIZE = Integer.BYTES;
    static final int LENGTH_SIZE = Integer.BYTES;
    static final int POINTER_SIZE = Long.BYTES;

    /**
     * We add some constraints on the size of the keys and values.
     * So that a node with a single KV pair always fits on a single page.
     * If you need to support bigger keys or bigger values,
     * you have to allocate extra pages for them and that adds complexity.
     */
    static final int MAX_KEY_SIZE = 1000;
    static final int MAX_VALUE_SIZE = 3000;

    static final byte[] EMPTY_BYTES = new byte[0];

    static final BTreeNode EMPTY_LEAF_NODE = BTreeNode.of(HEADER_SIZE, BTreeNodeType.LEAF, 0);
    static final BTreeNode EMPTY_INTERNAL_NODE = BTreeNode.of(HEADER_SIZE, BTreeNodeType.INTERNAL, 0);

    static final BTreeNode DEFAULT_ROOT_NODE = BTreeNode.of(HEADER_SIZE + OFFSET_SIZE + LENGTH_SIZE, BTreeNodeType.LEAF, 1);

    static {
        DEFAULT_ROOT_NODE.appendValue(0, EMPTY_BYTES, EMPTY_BYTES);
    }
}
