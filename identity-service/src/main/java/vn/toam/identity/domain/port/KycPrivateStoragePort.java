package vn.toam.identity.domain.port;

public interface KycPrivateStoragePort {

    String store(String key, byte[] content, String contentType);

    byte[] load(String key);

    void delete(String key);

    boolean exists(String key);
}

