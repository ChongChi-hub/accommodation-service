package com.service.room.repository;

import com.service.room.entity.ProvinceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProvinceRepository extends JpaRepository<ProvinceEntity, String> {

    @Query("SELECT p FROM ProvinceEntity p ORDER BY p.name ASC")
    List<ProvinceEntity> findAllOrderByName();
}

