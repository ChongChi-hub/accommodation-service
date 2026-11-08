package vn.cozynest.listing.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.cozynest.listing.domain.model.Bookmark;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    boolean existsByUserIdAndRoomId(String userId, Long roomId);

    Optional<Bookmark> findByUserIdAndRoomId(String userId, Long roomId);

    @Modifying
    @Query("DELETE FROM Bookmark b WHERE b.userId = :userId AND b.roomId = :roomId")
    int deleteByUserIdAndRoomId(@Param("userId") String userId, @Param("roomId") Long roomId);

    List<Bookmark> findByUserIdOrderByCreatedAtDesc(String userId);

    @Query("SELECT b.roomId FROM Bookmark b WHERE b.userId = :userId ORDER BY b.createdAt DESC")
    List<Long> findRoomIdsByUserId(@Param("userId") String userId);

    long countByRoomId(Long roomId);
}

