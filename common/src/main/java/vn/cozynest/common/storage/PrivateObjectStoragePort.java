package vn.cozynest.common.storage;

/**
 * Port for storing and retrieving sensitive documents (KYC, ownership docs, contracts) in private storage.
 */
public interface PrivateObjectStoragePort {

    StoredObjectMetadata storePrivate(String bucket, String objectKey, byte[] rawBytes, String contentType);

    byte[] loadPrivate(String bucket, String objectKey);

    void deletePrivate(String bucket, String objectKey);

    boolean exists(String bucket, String objectKey);

    record StoredObjectMetadata(String bucket, String objectKey, String sha256Hex, long sizeBytes, String contentType) {
    }
}

