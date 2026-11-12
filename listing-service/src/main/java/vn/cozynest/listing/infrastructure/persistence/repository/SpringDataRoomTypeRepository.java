package vn.cozynest.listing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.cozynest.listing.infrastructure.persistence.entity.RoomTypeJpaEntity;

import java.util.List;
import java.util.Optional;

public interface SpringDataRoomTypeRepository extends JpaRepository<RoomTypeJpaEntity, Long> {
    List<RoomTypeJpaEntity> findAllByOrderByIdAsc();
    Optional<RoomTypeJpaEntity> findByCodeIgnoreCase(String code);
}

