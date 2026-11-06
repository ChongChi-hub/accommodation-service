package com.service.room.repository;

import com.service.room.entity.DistrictEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DistrictRepository extends JpaRepository<DistrictEntity, String> {
    List<DistrictEntity> findByProvinceIdOrderByName(String provinceId);
}

