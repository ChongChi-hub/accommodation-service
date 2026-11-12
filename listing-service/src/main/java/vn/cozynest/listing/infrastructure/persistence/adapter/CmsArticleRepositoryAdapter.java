package vn.cozynest.listing.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vn.cozynest.listing.domain.model.CmsArticle;
import vn.cozynest.listing.domain.repository.CmsArticleRepository;
import vn.cozynest.listing.infrastructure.persistence.mapper.ListingCatalogPersistenceMapper;
import vn.cozynest.listing.infrastructure.persistence.repository.SpringDataCmsArticleRepository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CmsArticleRepositoryAdapter implements CmsArticleRepository {

    private final SpringDataCmsArticleRepository springRepo;
    private final ListingCatalogPersistenceMapper mapper;

    @Override
    public List<CmsArticle> findByActiveTrueOrderBySortOrderAscPublishedAtDesc() {
        return springRepo.findByActiveTrueOrderBySortOrderAscPublishedAtDesc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<CmsArticle> findBySlugAndActiveTrue(String slug) {
        return springRepo.findBySlugAndActiveTrue(slug).map(mapper::toDomain);
    }

    @Override
    public CmsArticle save(CmsArticle article) {
        return mapper.toDomain(springRepo.save(mapper.toEntity(article)));
    }
}
