package vn.cozynest.listing.domain.repository;

import vn.cozynest.listing.domain.model.Review;

import java.util.List;

public interface ReviewRepository {
    List<Review> findByRoomIdOrderByCreatedAtDesc(Long roomId);
    List<Review> findByRoomIdInOrderByCreatedAtDesc(List<Long> roomIds);
    long count();
    Review save(Review review);
}
