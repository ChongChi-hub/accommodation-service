package com.service.room.repository;

import com.service.room.entity.WardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WardRepository extends JpaRepository<WardEntity, String> {
    List<WardEntity> findByDistrictIdOrderByName(String districtId);
}

