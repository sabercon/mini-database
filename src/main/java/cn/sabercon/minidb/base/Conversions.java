package cn.sabercon.minidb.base;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class Conversions {

    private Conversions() {
        throw new UnsupportedOperationException();
    }

    public static long toLong(byte[] value) {
        return ByteBuffer.wrap(value).getLong();
    }

    public static byte[] toBytes(long value) {
        return ByteBuffer.allocate(Long.BYTES).putLong(value).array();
    }

    public static String toString(byte[] value) {
        return new String(value, StandardCharsets.UTF_8);
    }

    public static byte[] toBytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
