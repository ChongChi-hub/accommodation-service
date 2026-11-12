package vn.cozynest.listing.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vn.cozynest.listing.domain.model.Bookmark;
import vn.cozynest.listing.domain.repository.BookmarkRepository;
import vn.cozynest.listing.infrastructure.persistence.mapper.ListingCatalogPersistenceMapper;
import vn.cozynest.listing.infrastructure.persistence.repository.SpringDataBookmarkRepository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BookmarkRepositoryAdapter implements BookmarkRepository {

    private final SpringDataBookmarkRepository springRepo;
    private final ListingCatalogPersistenceMapper mapper;

    @Override
    public boolean existsByUserIdAndRoomId(String userId, Long roomId) {
        return springRepo.existsByUserIdAndRoomId(userId, roomId);
    }

    @Override
    public Optional<Bookmark> findByUserIdAndRoomId(String userId, Long roomId) {
        return springRepo.findByUserIdAndRoomId(userId, roomId).map(mapper::toDomain);
    }

    @Override
    public int deleteByUserIdAndRoomId(String userId, Long roomId) {
        return springRepo.deleteByUserIdAndRoomId(userId, roomId);
    }

    @Override
    public List<Bookmark> findByUserIdOrderByCreatedAtDesc(String userId) {
        return springRepo.findByUserIdOrderByCreatedAtDesc(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Long> findRoomIdsByUserId(String userId) {
        return springRepo.findRoomIdsByUserId(userId);
    }

    @Override
    public long countByRoomId(Long roomId) {
        return springRepo.countByRoomId(roomId);
    }

    @Override
    public long countByUserId(String userId) {
        return springRepo.countByUserId(userId);
    }

    @Override
    public Bookmark save(Bookmark bookmark) {
        return mapper.toDomain(springRepo.save(mapper.toEntity(bookmark)));
    }
}
