package vn.cozynest.listing.domain.repository;

import vn.cozynest.listing.domain.model.StorageAccessLog;

import java.util.List;

public interface StorageAccessLogRepository {

    StorageAccessLog save(StorageAccessLog log);

    List<StorageAccessLog> findByObjectKey(String objectKey);
}
