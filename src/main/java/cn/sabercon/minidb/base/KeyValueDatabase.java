package cn.sabercon.minidb.base;

import java.util.Optional;

public interface KeyValueDatabase {

    Optional<byte[]> find(byte[] key);

    void upsert(byte[] key, byte[] value);

    boolean delete(byte[] key);
}
