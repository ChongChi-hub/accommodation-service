package com.service.payment.repository;

import com.service.payment.entity.UtilityReadingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UtilityReadingRepository extends JpaRepository<UtilityReadingEntity, String> {
    List<UtilityReadingEntity> findByContractIdOrderByPeriodMonthDesc(String contractId);
}

