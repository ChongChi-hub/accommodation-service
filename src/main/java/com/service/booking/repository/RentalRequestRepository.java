package com.service.booking.repository;

import com.service.booking.entity.RentalRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RentalRequestRepository extends JpaRepository<RentalRequestEntity, String> {
    List<RentalRequestEntity> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    Optional<RentalRequestEntity> findByRoomIdAndTenantIdAndStatus(String roomId, String tenantId, String status);
}

