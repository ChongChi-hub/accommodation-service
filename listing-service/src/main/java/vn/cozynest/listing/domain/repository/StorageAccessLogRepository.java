package vn.cozynest.listing.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.cozynest.listing.domain.model.StorageAccessLog;

public interface StorageAccessLogRepository extends JpaRepository<StorageAccessLog, String> {
}

