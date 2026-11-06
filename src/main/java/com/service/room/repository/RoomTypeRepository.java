package com.service.room.repository;

import com.service.room.entity.RoomTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomTypeRepository extends JpaRepository<RoomTypeEntity, String> {
    Optional<RoomTypeEntity> findBySlug(String slug);
}

