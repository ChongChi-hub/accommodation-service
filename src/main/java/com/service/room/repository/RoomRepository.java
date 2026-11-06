package com.service.room.repository;

import com.service.room.entity.RoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<RoomEntity, String> {
    Optional<RoomEntity> findBySlug(String slug);
    List<RoomEntity> findByLandlordIdOrderByCreatedAtDesc(String landlordId);
}

