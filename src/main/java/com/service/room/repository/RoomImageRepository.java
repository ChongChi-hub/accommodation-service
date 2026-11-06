package com.service.room.repository;

import com.service.room.entity.RoomImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomImageRepository extends JpaRepository<RoomImageEntity, String> {
    List<RoomImageEntity> findByRoomIdOrderBySortOrderAsc(String roomId);
    void deleteByRoomId(String roomId);
}

