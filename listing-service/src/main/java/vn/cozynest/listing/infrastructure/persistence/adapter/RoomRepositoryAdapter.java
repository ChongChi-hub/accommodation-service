package vn.cozynest.listing.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import vn.cozynest.listing.domain.model.DomainPage;
import vn.cozynest.listing.domain.model.Room;
import vn.cozynest.listing.domain.model.RoomSearchQuery;
import vn.cozynest.listing.domain.repository.ProvinceRepository;
import vn.cozynest.listing.domain.repository.RoomRepository;
import vn.cozynest.listing.infrastructure.persistence.entity.RoomJpaEntity;
import vn.cozynest.listing.infrastructure.persistence.mapper.RoomPersistenceMapper;
import vn.cozynest.listing.infrastructure.persistence.repository.SpringDataRoomRepository;
import vn.cozynest.listing.infrastructure.persistence.specification.RoomSpecificationBuilder;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RoomRepositoryAdapter implements RoomRepository {

    private final SpringDataRoomRepository springRepo;
    private final RoomPersistenceMapper mapper;
    private final RoomSpecificationBuilder specificationBuilder;
    private final ProvinceRepository provinceRepository;

    @Override
    public DomainPage<Room> searchRooms(RoomSearchQuery query, int page, int size, String sortKey) {
        Sort sort = resolveSearchSort(sortKey);
        Pageable pageable = PageRequest.of(page, size, sort);
        Specification<RoomJpaEntity> spec = specificationBuilder.buildSpecification(query, provinceRepository);
        Page<RoomJpaEntity> result = springRepo.findAll(spec, pageable);
        return toDomainPage(result);
    }

    @Override
    public List<Room> findByStatusAndApprovedTrue(String status, int page, int size) {
        return springRepo.findByStatusAndApprovedTrue(status, PageRequest.of(page, size)).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<Room> findRecommendations(String provinceCode, Long roomId, int limit) {
        return springRepo.findRecommendations(provinceCode, roomId, PageRequest.of(0, limit)).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public DomainPage<Room> findByLandlordId(Long landlordId, int page, int size) {
        return toDomainPage(springRepo.findByLandlordId(landlordId, PageRequest.of(page, size)));
    }

    @Override
    public Optional<Room> findById(Long id) {
        return springRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Room> findAll() {
        return springRepo.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Room> findAllByIdIn(Collection<Long> ids) {
        return springRepo.findAllByIdIn(ids).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Room> findByIdAndLandlordIdAndDeletedAtIsNull(Long id, Long landlordId) {
        return springRepo.findByIdAndLandlordIdAndDeletedAtIsNull(id, landlordId).map(mapper::toDomain);
    }

    @Override
    public Optional<Room> findBySlugAndDeletedAtIsNull(String slug) {
        return springRepo.findBySlugAndDeletedAtIsNull(slug).map(mapper::toDomain);
    }

    @Override
    public Optional<Room> findByIdAndDeletedAtIsNull(Long id) {
        return springRepo.findByIdAndDeletedAtIsNull(id).map(mapper::toDomain);
    }

    @Override
    public Room save(Room room) {
        return mapper.toDomain(springRepo.save(mapper.toEntity(room)));
    }

    @Override
    public List<Room> findSimilarRooms(String provinceCode, Long roomId, BigDecimal minPrice, BigDecimal maxPrice, int limit) {
        return springRepo.findSimilarRooms(provinceCode, roomId, minPrice, maxPrice, PageRequest.of(0, limit))
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Object[]> findHotAreas() {
        return springRepo.findHotAreas();
    }

    @Override
    public long countActiveRooms() {
        return springRepo.countActiveRooms();
    }

    @Override
    public long countActiveProvinces() {
        return springRepo.countActiveProvinces();
    }

    @Override
    public long countVerifiedLandlords() {
        return springRepo.countVerifiedLandlords();
    }

    @Override
    public List<Object[]> countActiveByRoomTypeCode() {
        return springRepo.countActiveByRoomTypeCode();
    }

    @Override
    public List<Object[]> findActivePriceRange() {
        return springRepo.findActivePriceRange();
    }

    @Override
    public List<Object[]> countActiveByAmenity() {
        return springRepo.countActiveByAmenity();
    }

    @Override
    public List<Object[]> countActiveByProvince() {
        return springRepo.countActiveByProvince();
    }

    @Override
    public DomainPage<Room> findLandlordRooms(Long landlordId, String status, String search, int page, int size, String sortField, boolean ascending) {
        Sort.Direction direction = ascending ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(direction, sortField));
        return toDomainPage(springRepo.findLandlordRooms(landlordId, status, search, pageable));
    }

    @Override
    public long countByLandlordIdAndDeletedAtIsNull(Long landlordId) {
        return springRepo.countByLandlordIdAndDeletedAtIsNull(landlordId);
    }

    @Override
    public long countByLandlordIdAndDeletedAtIsNullAndStatus(Long landlordId, String status) {
        return springRepo.countByLandlordIdAndDeletedAtIsNullAndStatus(landlordId, status);
    }

    @Override
    public int incrementBookmarkCount(Long roomId) {
        return springRepo.incrementBookmarkCount(roomId);
    }

    @Override
    public int decrementBookmarkCount(Long roomId) {
        return springRepo.decrementBookmarkCount(roomId);
    }

    private DomainPage<Room> toDomainPage(Page<RoomJpaEntity> page) {
        return new DomainPage<>(
                page.getContent().stream().map(mapper::toDomain).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
        );
    }

    private Sort resolveSearchSort(String sortKey) {
        if ("price_asc".equalsIgnoreCase(sortKey) || "priceAsc".equalsIgnoreCase(sortKey)) {
            return Sort.by(Sort.Order.asc("price"), Sort.Order.desc("id"));
        }
        if ("price_desc".equalsIgnoreCase(sortKey) || "priceDesc".equalsIgnoreCase(sortKey)) {
            return Sort.by(Sort.Order.desc("price"), Sort.Order.desc("id"));
        }
        if ("areaAsc".equalsIgnoreCase(sortKey)) {
            return Sort.by(Sort.Order.asc("area"), Sort.Order.desc("id"));
        }
        if ("areaDesc".equalsIgnoreCase(sortKey)) {
            return Sort.by(Sort.Order.desc("area"), Sort.Order.desc("id"));
        }
        if ("rating".equalsIgnoreCase(sortKey)) {
            return Sort.by(Sort.Order.desc("ratingAvg"), Sort.Order.desc("id"));
        }
        return Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
    }
}

