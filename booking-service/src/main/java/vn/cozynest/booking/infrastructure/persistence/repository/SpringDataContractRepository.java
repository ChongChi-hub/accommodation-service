package vn.cozynest.booking.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.cozynest.booking.infrastructure.persistence.entity.ContractJpaEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataContractRepository extends JpaRepository<ContractJpaEntity, String> {
    Optional<ContractJpaEntity> findByContractCode(String contractCode);
    List<ContractJpaEntity> findByTenantId(String tenantId);
    List<ContractJpaEntity> findByLandlordId(String landlordId);

    @Query("SELECT c FROM ContractJpaEntity c WHERE c.roomId = :roomId AND c.status = 'ACTIVE'")
    Optional<ContractJpaEntity> findActiveByRoomId(@Param("roomId") String roomId);

    @Query("SELECT c FROM ContractJpaEntity c WHERE c.endDate BETWEEN :start AND :end AND c.status = 'ACTIVE'")
    List<ContractJpaEntity> findExpiringBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);
}

