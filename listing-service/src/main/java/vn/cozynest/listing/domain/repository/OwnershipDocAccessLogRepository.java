package vn.cozynest.listing.domain.repository;

import vn.cozynest.listing.domain.model.OwnershipDocAccessLog;

import java.util.List;

public interface OwnershipDocAccessLogRepository {
    OwnershipDocAccessLog save(OwnershipDocAccessLog entry);

    List<OwnershipDocAccessLog> findByRoomId(Long roomId);
}

