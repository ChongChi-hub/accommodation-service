package vn.cozynest.identity.infrastructure.persistence.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.cozynest.identity.domain.model.KycStatus;
import vn.cozynest.identity.infrastructure.persistence.entity.UserVerificationJpaEntity;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataUserVerificationRepository extends JpaRepository<UserVerificationJpaEntity, String> {

    Optional<UserVerificationJpaEntity> findByUserId(String userId);

    boolean existsByStatusInAndIdNumberHash(Collection<KycStatus> statuses, String idNumberHash);

    boolean existsByStatusInAndIdNumberHashAndUserIdNot(Collection<KycStatus> statuses, String idNumberHash, String userId);

    @Query("SELECT v FROM UserVerificationJpaEntity v WHERE " +
           "(:status IS NULL OR v.status = :status) AND " +
           "(:search IS NULL OR LOWER(v.fullNameOnId) LIKE LOWER(CONCAT('%', :search, '%')) OR v.idNumberLast4 LIKE CONCAT('%', :search, '%')) " +
           "ORDER BY v.submittedAt DESC")
    List<UserVerificationJpaEntity> findFiltered(
            @Param("status") KycStatus status,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT COUNT(v) FROM UserVerificationJpaEntity v WHERE " +
           "(:status IS NULL OR v.status = :status) AND " +
           "(:search IS NULL OR LOWER(v.fullNameOnId) LIKE LOWER(CONCAT('%', :search, '%')) OR v.idNumberLast4 LIKE CONCAT('%', :search, '%'))")
    long countFiltered(
            @Param("status") KycStatus status,
            @Param("search") String search);

    List<UserVerificationJpaEntity> findByStatusAndUpdatedAtBefore(KycStatus status, Instant cutoff);
}

