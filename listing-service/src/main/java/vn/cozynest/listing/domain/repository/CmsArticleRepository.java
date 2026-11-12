package vn.cozynest.listing.domain.repository;

import vn.cozynest.listing.domain.model.CmsArticle;

import java.util.List;
import java.util.Optional;

public interface CmsArticleRepository {
    List<CmsArticle> findByActiveTrueOrderBySortOrderAscPublishedAtDesc();
    Optional<CmsArticle> findBySlugAndActiveTrue(String slug);
    CmsArticle save(CmsArticle article);
}
