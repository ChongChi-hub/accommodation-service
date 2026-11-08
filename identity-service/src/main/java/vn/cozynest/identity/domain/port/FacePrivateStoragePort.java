package vn.cozynest.identity.domain.port;

public interface FacePrivateStoragePort {
    void store(String storageKey, byte[] encryptedBytes);
    byte[] load(String storageKey);
    void delete(String storageKey);
}

