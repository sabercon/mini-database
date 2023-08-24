package cn.sabercon.minidb.btree;

import cn.sabercon.minidb.TestPageStore;
import cn.sabercon.minidb.TestUtils;
import cn.sabercon.minidb.base.Pair;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cn.sabercon.minidb.TestUtils.randomBytes;
import static org.junit.jupiter.api.Assertions.*;

class BTreeTest {

    private BTree testBtree() {
        return new BTree(new TestPageStore());
    }

    @Nested
    class Find {

        @Test
        void returns_error_when_key_is_empty() {
            var btree = testBtree();

            assertThrows(IllegalArgumentException.class, () -> btree.find(new byte[0]));
        }

        @Test
        void returns_empty_when_key_does_not_exist() {
            var btree = testBtree();
            var key = randomBytes();

            assertTrue(btree.find(key).isEmpty());
        }

        @Test
        void returns_value_when_key_exists() {
            var btree = testBtree();
            var key = randomBytes();
            var value = randomBytes();
            btree.upsert(key, value);

            var result = btree.find(key);
            assertTrue(result.isPresent());
            assertArrayEquals(value, result.get());
        }
    }

    @Nested
    class Upsert {

        @Test
        void returns_error_when_key_is_empty() {
            var btree = testBtree();
            var value = randomBytes();

            assertThrows(IllegalArgumentException.class, () -> btree.upsert(new byte[0], value));
        }

        @Test
        void returns_error_when_key_is_too_big() {
            var btree = testBtree();
            var key = randomBytes(4000);
            var value = randomBytes(1);

            assertThrows(IllegalArgumentException.class, () -> btree.upsert(key, value));
        }

        @Test
        void returns_error_when_value_is_too_big() {
            var btree = testBtree();
            var key = randomBytes(1);
            var value = randomBytes(4000);

            assertThrows(IllegalArgumentException.class, () -> btree.upsert(key, value));
        }

        @Test
        void inserts_value_when_key_does_not_exist() {
            var btree = testBtree();
            var key1 = randomBytes(100);
            var key2 = randomBytes(200);
            var value1 = randomBytes(100);
            var value2 = randomBytes(200);

            btree.upsert(key1, value1);
            btree.upsert(key2, value2);

            var result = btree.find(key2);
            assertTrue(result.isPresent());
            assertArrayEquals(value2, result.get());
        }

        @Test
        void updates_value_when_key_exists() {
            var btree = testBtree();
            var key = randomBytes();
            var value1 = randomBytes(100);
            var value2 = randomBytes(200);

            btree.upsert(key, value1);
            btree.upsert(key, value2);

            var result = btree.find(key);
            assertTrue(result.isPresent());
            assertArrayEquals(value2, result.get());
        }

        @Test
        void works_as_expected_when_inserting_many_keys() {
            var btree = testBtree();
            var keyMap = Stream.generate(() -> Pair.of(ByteBuffer.wrap(randomBytes()), randomBytes()))
                    .limit(1000)
                    .collect(Collectors.toMap(Pair::first, Pair::second, (a, b) -> a));

            keyMap.forEach((key, value) -> btree.upsert(key.array(), value));

            keyMap.forEach((key, value) -> {
                var result = btree.find(key.array());
                assertTrue(result.isPresent());
                assertArrayEquals(value, result.get());
            });
        }

        @Test
        void works_as_expected_when_inserting_many_small_keys() {
            var btree = testBtree();
            var keyMap = Stream.generate(() -> Pair.of(ByteBuffer.wrap(randomBytes(1)), randomBytes(1)))
                    .limit(1000)
                    .collect(Collectors.toMap(Pair::first, Pair::second, (a, b) -> a));

            keyMap.forEach((key, value) -> btree.upsert(key.array(), value));

            keyMap.forEach((key, value) -> {
                var result = btree.find(key.array());
                assertTrue(result.isPresent());
                assertArrayEquals(value, result.get());
            });
        }

        @Test
        void works_as_expected_when_inserting_many_big_keys() {
            var btree = testBtree();
            var keyMap = Stream.generate(() -> Pair.of(ByteBuffer.wrap(randomBytes(1000)), randomBytes(3000)))
                    .limit(1000)
                    .collect(Collectors.toMap(Pair::first, Pair::second, (a, b) -> a));

            keyMap.forEach((key, value) -> btree.upsert(key.array(), value));

            keyMap.forEach((key, value) -> {
                var result = btree.find(key.array());
                assertTrue(result.isPresent());
                assertArrayEquals(value, result.get());
            });
        }

        @Test
        void works_as_expected_when_updating_many_keys() {
            var btree = testBtree();
            var keyMap = Stream.generate(() -> Pair.of(ByteBuffer.wrap(randomBytes()), randomBytes()))
                    .limit(1000)
                    .collect(Collectors.toMap(Pair::first, Pair::second, (a, b) -> a));

            keyMap.forEach((key, value) -> btree.upsert(key.array(), randomBytes()));
            keyMap.forEach((key, value) -> btree.upsert(key.array(), value));

            keyMap.forEach((key, value) -> {
                var result = btree.find(key.array());
                assertTrue(result.isPresent());
                assertArrayEquals(value, result.get());
            });
        }
    }

    @Nested
    class Delete {

        @Test
        void returns_error_when_key_is_empty() {
            var btree = testBtree();

            assertThrows(IllegalArgumentException.class, () -> btree.delete(new byte[0]));
        }

        @Test
        void returns_false_when_key_does_not_exist() {
            var btree = testBtree();
            var key = randomBytes();

            assertFalse(btree.delete(key));
            assertTrue(btree.find(key).isEmpty());
        }

        @Test
        void returns_true_when_key_exists() {
            var btree = testBtree();
            var key = randomBytes();
            btree.upsert(key, randomBytes());

            assertTrue(btree.delete(key));
            assertTrue(btree.find(key).isEmpty());
        }

        @Test
        void returns_false_when_key_is_deleted() {
            var btree = testBtree();
            var key = randomBytes();
            btree.upsert(key, randomBytes());

            assertTrue(btree.delete(key));
            assertFalse(btree.delete(key));
            assertTrue(btree.find(key).isEmpty());
        }

        @Test
        void works_as_expected_when_deleting_many_keys() {
            var btree = testBtree();
            var keySet = Stream.generate(() -> ByteBuffer.wrap(randomBytes()))
                    .limit(1000)
                    .collect(Collectors.toSet());
            keySet.forEach(key -> btree.upsert(key.array(), randomBytes()));

            keySet.forEach(key -> assertTrue(btree.delete(key.array())));
            keySet.forEach(key -> assertTrue(btree.find(key.array()).isEmpty()));
        }

        @Test
        void works_as_expected_when_deleting_many_small_keys() {
            var btree = testBtree();
            var keySet = Stream.generate(() -> ByteBuffer.wrap(randomBytes(1)))
                    .limit(1000)
                    .collect(Collectors.toSet());
            keySet.forEach(key -> btree.upsert(key.array(), randomBytes(1)));

            keySet.forEach(key -> assertTrue(btree.delete(key.array())));
            keySet.forEach(key -> assertTrue(btree.find(key.array()).isEmpty()));
        }

        @Test
        void works_as_expected_when_deleting_many_big_keys() {
            var btree = testBtree();
            var keySet = Stream.generate(() -> ByteBuffer.wrap(randomBytes(1000)))
                    .limit(1000)
                    .collect(Collectors.toSet());
            keySet.forEach(key -> btree.upsert(key.array(), randomBytes(3000)));

            keySet.forEach(key -> assertTrue(btree.delete(key.array())));
            keySet.forEach(key -> assertTrue(btree.find(key.array()).isEmpty()));
        }
    }
}
