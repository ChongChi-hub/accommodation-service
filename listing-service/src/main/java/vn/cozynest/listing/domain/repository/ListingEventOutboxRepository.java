package vn.cozynest.listing.domain.repository;

import vn.cozynest.listing.domain.model.ListingEventOutbox;

import java.util.List;
import java.util.Optional;

public interface ListingEventOutboxRepository {

    ListingEventOutbox save(ListingEventOutbox outbox);

    List<ListingEventOutbox> findPending(int limit);

    Optional<ListingEventOutbox> findById(String id);
}
