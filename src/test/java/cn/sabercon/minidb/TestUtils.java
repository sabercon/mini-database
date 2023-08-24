package cn.sabercon.minidb;

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
}
