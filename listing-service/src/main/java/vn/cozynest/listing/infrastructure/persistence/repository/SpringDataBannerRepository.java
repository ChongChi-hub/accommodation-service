package vn.cozynest.listing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.cozynest.listing.infrastructure.persistence.entity.BannerJpaEntity;

import java.util.List;

public interface SpringDataBannerRepository extends JpaRepository<BannerJpaEntity, Long> {
    List<BannerJpaEntity> findByActiveTrueOrderBySortOrderAsc();
}

