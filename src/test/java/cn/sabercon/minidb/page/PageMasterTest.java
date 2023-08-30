package cn.sabercon.minidb.page;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageMasterTest {

    @Test
    void signature_size_should_be_16() {
        assertEquals(16, PageMaster.SIGNATURE.byteSize());
    }
}
