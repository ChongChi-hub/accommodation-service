package vn.cozynest.listing.domain.repository;

import vn.cozynest.listing.domain.model.Bookmark;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository {
    boolean existsByUserIdAndRoomId(String userId, Long roomId);
    Optional<Bookmark> findByUserIdAndRoomId(String userId, Long roomId);
    int deleteByUserIdAndRoomId(String userId, Long roomId);
    List<Bookmark> findByUserIdOrderByCreatedAtDesc(String userId);
    List<Long> findRoomIdsByUserId(String userId);
    long countByRoomId(Long roomId);
    long countByUserId(String userId);
    Bookmark save(Bookmark bookmark);
}
