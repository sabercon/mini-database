package cn.sabercon.minidb.btree;

import org.junit.jupiter.api.Test;

import static cn.sabercon.minidb.btree.BTreeConstants.*;
import static cn.sabercon.minidb.page.PageConstants.HEADER_SIZE;
import static cn.sabercon.minidb.page.PageConstants.PAGE_BYTE_SIZE;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BTreeConstantsTest {

    @Test
    @SuppressWarnings("ConstantValue")
    void max_single_key_node_size_should_not_exceed_page_size() {
        var maxSingleKeyNodeSize = HEADER_SIZE + OFFSET_SIZE + LENGTH_SIZE + MAX_KEY_SIZE + MAX_VALUE_SIZE;
        assertTrue(maxSingleKeyNodeSize <= PAGE_BYTE_SIZE);
    }
}
