package com.service.booking.repository;

import com.service.booking.entity.ContractEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractRepository extends JpaRepository<ContractEntity, String> {
    List<ContractEntity> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    List<ContractEntity> findByLandlordIdOrderByCreatedAtDesc(String landlordId);
}

