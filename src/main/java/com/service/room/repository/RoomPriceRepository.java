package com.service.room.repository;

import com.service.room.entity.RoomPriceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomPriceRepository extends JpaRepository<RoomPriceEntity, String> {
    List<RoomPriceEntity> findByRoomId(String roomId);
    void deleteByRoomId(String roomId);
}

