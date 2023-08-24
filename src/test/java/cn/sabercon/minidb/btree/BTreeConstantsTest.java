package cn.sabercon.minidb.btree;

import org.junit.jupiter.api.Test;

import static cn.sabercon.minidb.base.PageNode.PAGE_SIZE;
import static cn.sabercon.minidb.btree.BTreeConstants.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BTreeConstantsTest {

    @Test
    @SuppressWarnings("ConstantValue")
    void max_single_key_node_size_should_not_exceed_page_size() {
        var maxSingleKeyNodeSize = HEADER_SIZE + OFFSET_SIZE + LENGTH_SIZE + MAX_KV_SIZE;
        assertTrue(maxSingleKeyNodeSize <= PAGE_SIZE);
    }
}
