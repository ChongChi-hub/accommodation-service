package com.service.room.repository;

import com.service.room.entity.BookmarkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<BookmarkEntity, String> {
    Optional<BookmarkEntity> findByUserIdAndRoomId(String userId, String roomId);
    boolean existsByUserIdAndRoomId(String userId, String roomId);
    void deleteByUserIdAndRoomId(String userId, String roomId);
    List<BookmarkEntity> findByUserIdOrderByCreatedAtDesc(String userId);
}

