package vn.cozynest.listing.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vn.cozynest.listing.domain.model.Review;
import vn.cozynest.listing.domain.repository.ReviewRepository;
import vn.cozynest.listing.infrastructure.persistence.mapper.ListingCatalogPersistenceMapper;
import vn.cozynest.listing.infrastructure.persistence.repository.SpringDataReviewRepository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ReviewRepositoryAdapter implements ReviewRepository {

    private final SpringDataReviewRepository springRepo;
    private final ListingCatalogPersistenceMapper mapper;

    @Override
    public List<Review> findByRoomIdOrderByCreatedAtDesc(Long roomId) {
        return springRepo.findByRoomIdOrderByCreatedAtDesc(roomId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public long count() {
        return springRepo.count();
    }

    @Override
    public Review save(Review review) {
        return mapper.toDomain(springRepo.save(mapper.toEntity(review)));
    }
}
