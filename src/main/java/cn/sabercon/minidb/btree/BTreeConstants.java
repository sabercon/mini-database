package cn.sabercon.minidb.btree;

import cn.sabercon.minidb.page.PageType;

final class BTreeConstants {

    private BTreeConstants() {
        throw new UnsupportedOperationException();
    }

    static final int OFFSET_SIZE = Integer.BYTES;
    static final int LENGTH_SIZE = Integer.BYTES;

    /**
     * We add some constraints on the size of the keys and values.
     * So that a node with a single KV pair always fits on a single page.
     * If you need to support bigger keys or bigger values,
     * you have to allocate extra pages for them and that adds complexity.
     */
    static final int MAX_KEY_SIZE = 1000;
    static final int MAX_VALUE_SIZE = 3000;

    static final BTreeNode DEFAULT_ROOT_NODE;

    static {
        var rootNode = BTreeNode.of(PageType.BTREE_LEAF, 1, 1);
        var emptyBytes = new byte[0];
        rootNode.appendValue(0, emptyBytes, emptyBytes);
        DEFAULT_ROOT_NODE = rootNode;
    }
}
