package cn.sabercon.minidb.base;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.foreign.MemorySegment;
import java.nio.file.Path;

import static cn.sabercon.minidb.TestUtils.*;
import static cn.sabercon.minidb.base.DefaultFileBuffer.MIN_BYTE_SIZE;
import static org.junit.jupiter.api.Assertions.*;

class DefaultFileBufferTest {

    FileBuffer buffer;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        buffer = FileBuffer.from(tempDir.resolve("test.minidb"));
    }

    @AfterEach
    void tearDown() {
        buffer = null;
    }

    @Nested
    class Get {

        @Test
        void returns_error_when_file_is_empty() {
            assertThrows(IllegalArgumentException.class, () -> buffer.get(0, 1));
        }

        @Test
        void returns_error_when_out_of_bounds() {
            buffer.set(0, MemorySegment.ofArray(randomBytes()));
            var size = buffer.byteSize();
            assertThrows(IllegalArgumentException.class, () -> buffer.get(size, 1));
        }

        @RepeatedTest(10)
        void returns_data_set_when_size_the_same() {
            var index = randomLong(0, MIN_BYTE_SIZE * 2);
            var data = MemorySegment.ofArray(randomBytes(4096));
            buffer.set(index, data);

            assertSegmentEquals(data, buffer.get(index, 4096));
        }

        @RepeatedTest(10)
        void returns_part_of_data_set_when_size_smaller() {
            var index = randomLong(0, MIN_BYTE_SIZE * 2);
            var data = MemorySegment.ofArray(randomBytes(4096));
            buffer.set(index, data);

            assertSegmentEquals(data.asSlice(0, 2048), buffer.get(index, 2048));
        }
    }

    @Nested
    class Set {

        @ParameterizedTest
        @ValueSource(longs = {0, 1, 100, MIN_BYTE_SIZE, 10 * MIN_BYTE_SIZE})
        void inserts_data_when_index_not_used(long index) {
            var data = MemorySegment.ofArray(randomBytes(4096));
            buffer.set(index, data);

            assertSegmentEquals(data, buffer.get(index, data.byteSize()));
        }

        @ParameterizedTest
        @ValueSource(longs = {0, 1, 100, MIN_BYTE_SIZE, 10 * MIN_BYTE_SIZE})
        void updates_data_when_index_used(long index) {
            var data1 = MemorySegment.ofArray(randomBytes(4096));
            var data2 = MemorySegment.ofArray(randomBytes(4096));
            buffer.set(index, data1);
            buffer.set(index, data2);

            assertSegmentEquals(data2, buffer.get(index, data2.byteSize()));
        }
    }

    @Nested
    class ByteSize {

        @Test
        void returns_zero_when_no_data_set() {
            assertEquals(0, buffer.byteSize());
        }

        @Test
        void returns_default_size_after_data_set_in_small_range() {
            var offset = 0;
            var data = MemorySegment.ofArray(randomBytes());
            buffer.set(offset, data);
            assertEquals(MIN_BYTE_SIZE, buffer.byteSize());
        }

        @Test
        void returns_bigger_size_after_data_set_in_big_range() {
            var offset = MIN_BYTE_SIZE + randomLong(0, MIN_BYTE_SIZE);
            var data = MemorySegment.ofArray(randomBytes());
            buffer.set(offset, data);

            assertTrue(buffer.byteSize() >= offset + data.byteSize());
        }
    }

    @Nested
    class Flush {

        @Test
        void succeeds_when_invoked() {
            assertDoesNotThrow(buffer::flush);
        }
    }
}
