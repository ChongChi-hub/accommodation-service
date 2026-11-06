package com.service.room.repository;

import com.service.room.entity.ReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<ReviewEntity, String> {
    Optional<ReviewEntity> findByRoomIdAndTenantId(String roomId, String tenantId);
    List<ReviewEntity> findByRoomIdOrderByCreatedAtDesc(String roomId);

    @Query("SELECT AVG(r.rating) FROM ReviewEntity r WHERE r.roomId = :roomId")
    Double getAverageRating(@Param("roomId") String roomId);

    @Query("SELECT COUNT(r) FROM ReviewEntity r WHERE r.roomId = :roomId")
    Long countReviews(@Param("roomId") String roomId);
}

