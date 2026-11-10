package vn.cozynest.billing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.cozynest.billing.infrastructure.persistence.entity.PayoutRequestJpaEntity;

import java.util.List;
import java.util.Optional;

public interface SpringDataPayoutRequestRepository extends JpaRepository<PayoutRequestJpaEntity, String> {
    Optional<PayoutRequestJpaEntity> findByPayoutCode(String payoutCode);
    List<PayoutRequestJpaEntity> findByLandlordIdOrderByCreatedAtDesc(String landlordId);
}

