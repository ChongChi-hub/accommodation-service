package com.service.payment.repository;

import com.service.payment.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, String> {
    List<PaymentEntity> findByInvoiceId(String invoiceId);
    List<PaymentEntity> findByTenantIdOrderByPaidAtDesc(String tenantId);
}

