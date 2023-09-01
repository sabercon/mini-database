package cn.sabercon.minidb.page;

import cn.sabercon.minidb.base.FileBuffer;
import cn.sabercon.minidb.util.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.foreign.MemorySegment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cn.sabercon.minidb.TestUtils.assertSegmentEquals;
import static cn.sabercon.minidb.TestUtils.randomBytes;
import static cn.sabercon.minidb.page.PageConstants.NULL_POINTER;
import static cn.sabercon.minidb.page.PageConstants.PAGE_BYTE_SIZE;
import static org.junit.jupiter.api.Assertions.*;

class DefaultPageManagerTest {

    PageManager manager;

    @BeforeEach
    void setUp() throws Exception {
        manager = PageManager.of(FileBuffer.from("test.minidb"));
    }

    @AfterEach
    void tearDown() throws Exception {
        manager = null;
        Files.delete(Path.of("test.minidb"));
    }

    @Nested
    class Root {

        @Test
        void root_should_be_null_by_default() {
            assertEquals(NULL_POINTER, manager.getRoot());
        }

        @ParameterizedTest
        @ValueSource(longs = {0, 1, 1000, Long.MAX_VALUE})
        void root_should_be_the_root_set(long root) {
            manager.setRoot(root);
            assertEquals(root, manager.getRoot());
        }
    }

    @Nested
    class Page {

        @Test
        void accessing_null_pointer_should_return_error() {
            assertThrows(IllegalArgumentException.class, () -> manager.getPage(NULL_POINTER));
        }

        @Test
        void accessing_out_of_bounds_pointer_should_return_error() {
            assertThrows(IllegalArgumentException.class, () -> manager.getPage(1));
        }

        @Test
        void created_page_should_be_accessible_before_flush() {
            var page = randomPage();
            var pointer = manager.createPage(page);
            assertSegmentEquals(page, manager.getPage(pointer));
        }

        @Test
        void created_page_should_be_accessible_after_flush() {
            var page = randomPage();
            var pointer = manager.createPage(page);
            manager.flush();
            assertSegmentEquals(page, manager.getPage(pointer));
        }

        @Test
        void works_as_expected_when_creating_a_lot_of_pages() {
            var pageMap = Stream.generate(() -> randomPage())
                    .limit(1000)
                    .map(page -> Pair.of(manager.createPage(page), page))
                    .collect(Collectors.toMap(Pair::first, Pair::second, (a, b) -> a));

            pageMap.forEach((pointer, page) -> assertSegmentEquals(page, manager.getPage(pointer)));
            manager.flush();
            pageMap.forEach((pointer, page) -> assertSegmentEquals(page, manager.getPage(pointer)));
        }

        @Test
        void deleted_page_should_be_reused_after_flush() {
            var pointer1 = manager.createPage(randomPage());
            manager.deletePage(pointer1);
            manager.flush();
            var pointer2 = manager.createPage(randomPage());
            assertEquals(pointer1, pointer2);
        }

        @Test
        void works_as_expected_when_deleting_a_lot_of_pages() {
            var deletedPointers = Stream.generate(() -> manager.createPage(randomPage()))
                    .limit(1000)
                    .peek(page -> manager.deletePage(page))
                    .collect(Collectors.toSet());
            manager.flush();

            var newPointers = Stream.generate(() -> manager.createPage(randomPage()))
                    .limit(1000)
                    .filter(pointer -> !deletedPointers.contains(pointer))
                    .toList();

            // The reuse rate should be higher than 99%.
            assertTrue(newPointers.size() < 10);
        }
    }

    @Nested
    class Flush {

        @Test
        void succeeds_when_empty() {
            assertDoesNotThrow(manager::flush);
        }
    }

    static MemorySegment randomPage() {
        var bytes = randomBytes(PAGE_BYTE_SIZE);
        return MemorySegment.ofArray(bytes);
    }
}
