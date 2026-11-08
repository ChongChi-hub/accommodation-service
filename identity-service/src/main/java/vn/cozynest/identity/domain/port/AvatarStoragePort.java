package vn.cozynest.identity.domain.port;

public interface AvatarStoragePort {

    String store(String key, byte[] content, String contentType);

    byte[] load(String key);

    void delete(String key);

    boolean exists(String key);
}

