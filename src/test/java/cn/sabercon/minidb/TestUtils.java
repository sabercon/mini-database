package cn.sabercon.minidb;

import org.junit.jupiter.api.Assertions;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.ThreadLocalRandom;

public final class TestUtils {

    private TestUtils() {
        throw new UnsupportedOperationException();
    }

    public static byte[] randomBytes(int length) {
        var bytes = new byte[length];
        ThreadLocalRandom.current().nextBytes(bytes);
        return bytes;
    }

    public static byte[] randomBytes(int minLength, int maxLength) {
        return randomBytes(ThreadLocalRandom.current().nextInt(minLength, maxLength + 1));
    }

    public static byte[] randomBytes() {
        return randomBytes(1, 1000);
    }

    public static int randomInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    public static long randomLong(long min, long max) {
        return ThreadLocalRandom.current().nextLong(min, max + 1);
    }

    public static void assertSegmentEquals(MemorySegment expected, MemorySegment actual) {
        Assertions.assertEquals(-1, expected.mismatch(actual));
    }
}
