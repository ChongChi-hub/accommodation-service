package vn.cozynest.listing.domain.port;

public interface ImageStoragePort {

    String store(String key, byte[] content, String contentType);

    byte[] load(String key);

    void delete(String key);

    boolean exists(String key);
}

