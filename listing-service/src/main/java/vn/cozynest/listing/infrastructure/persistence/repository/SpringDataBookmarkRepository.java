package vn.cozynest.listing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.cozynest.listing.infrastructure.persistence.entity.BookmarkJpaEntity;

import java.util.List;
import java.util.Optional;

public interface SpringDataBookmarkRepository extends JpaRepository<BookmarkJpaEntity, Long> {
    boolean existsByUserIdAndRoomId(String userId, Long roomId);
    Optional<BookmarkJpaEntity> findByUserIdAndRoomId(String userId, Long roomId);

    @Modifying
    @Query("DELETE FROM BookmarkJpaEntity b WHERE b.userId = :userId AND b.roomId = :roomId")
    int deleteByUserIdAndRoomId(@Param("userId") String userId, @Param("roomId") Long roomId);

    List<BookmarkJpaEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    @Query("SELECT b.roomId FROM BookmarkJpaEntity b WHERE b.userId = :userId ORDER BY b.createdAt DESC")
    List<Long> findRoomIdsByUserId(@Param("userId") String userId);

    long countByRoomId(Long roomId);
    long countByUserId(String userId);
}
