package cn.sabercon.minidb.base;

import java.util.Optional;

public interface KeyValueStore {

    Optional<byte[]> find(byte[] key);

    void upsert(byte[] key, byte[] value);

    boolean delete(byte[] key);
}
