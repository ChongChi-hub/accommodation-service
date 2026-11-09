package vn.cozynest.listing.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.cozynest.listing.domain.model.CmsArticle;

import java.util.List;
import java.util.Optional;

public interface CmsArticleRepository extends JpaRepository<CmsArticle, Long> {
    List<CmsArticle> findByActiveTrueOrderBySortOrderAscPublishedAtDesc();
    Optional<CmsArticle> findBySlugAndActiveTrue(String slug);
}

