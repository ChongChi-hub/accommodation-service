package vn.cozynest.listing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.cozynest.listing.infrastructure.persistence.entity.CmsArticleJpaEntity;

import java.util.List;
import java.util.Optional;

public interface SpringDataCmsArticleRepository extends JpaRepository<CmsArticleJpaEntity, Long> {
    List<CmsArticleJpaEntity> findByActiveTrueOrderBySortOrderAscPublishedAtDesc();
    Optional<CmsArticleJpaEntity> findBySlugAndActiveTrue(String slug);
}
