package vn.cozynest.listing.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.listing.application.dto.HostPublicProfileDto;
import vn.cozynest.listing.application.dto.RoomDto;
import vn.cozynest.listing.domain.model.LandlordSnapshot;
import vn.cozynest.listing.domain.model.Review;
import vn.cozynest.listing.domain.model.Room;
import vn.cozynest.listing.domain.repository.LandlordSnapshotRepository;
import vn.cozynest.listing.domain.repository.ReviewRepository;
import vn.cozynest.listing.domain.repository.RoomRepository;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class HostQueryService {

    private final LandlordSnapshotRepository landlordSnapshotRepository;
    private final RoomRepository roomRepository;
    private final RoomQueryService roomQueryService;
    private final ReviewRepository reviewRepository;

    public HostQueryService(LandlordSnapshotRepository landlordSnapshotRepository,
                            RoomRepository roomRepository,
                            RoomQueryService roomQueryService) {
        this(landlordSnapshotRepository, roomRepository, roomQueryService, null);
    }

    @Autowired
    public HostQueryService(LandlordSnapshotRepository landlordSnapshotRepository,
                            RoomRepository roomRepository,
                            RoomQueryService roomQueryService,
                            @Autowired(required = false) ReviewRepository reviewRepository) {
        this.landlordSnapshotRepository = landlordSnapshotRepository;
        this.roomRepository = roomRepository;
        this.roomQueryService = roomQueryService;
        this.reviewRepository = reviewRepository;
    }

    public HostPublicProfileDto getPublicHostProfile(Long landlordId) {
        Optional<LandlordSnapshot> snapshotOpt = landlordSnapshotRepository.findByLandlordId(landlordId);
        List<Room> publicRooms = roomRepository.findByLandlordId(landlordId, 0, 50).content().stream()
                .filter(this::isPublicRoom)
                .toList();

        if (snapshotOpt.isEmpty() && publicRooms.isEmpty()) {
            throw new NoSuchElementException("Không tìm thấy thông tin chủ trọ: " + landlordId);
        }

        List<RoomDto> roomDtos = roomQueryService.toRoomDtos(publicRooms);

        // Calculate aggregate average rating from all reviews across landlord's rooms
        double computedRatingAvg = 5.0;
        int computedReviewCount = 0;
        if (reviewRepository != null) {
            List<Long> allLandlordRoomIds = roomRepository.findByLandlordId(landlordId, 0, 100).content().stream()
                    .map(Room::getId)
                    .toList();
            if (!allLandlordRoomIds.isEmpty()) {
                List<Review> visibleReviews = reviewRepository.findByRoomIdInOrderByCreatedAtDesc(allLandlordRoomIds).stream()
                        .filter(r -> r.getStatus() == null || "VISIBLE".equalsIgnoreCase(r.getStatus()))
                        .toList();
                computedReviewCount = visibleReviews.size();
                if (computedReviewCount > 0) {
                    computedRatingAvg = Math.round(visibleReviews.stream().mapToInt(Review::getRating).average().orElse(5.0) * 10.0) / 10.0;
                }
            }
        } else if (snapshotOpt.isPresent() && snapshotOpt.get().getRatingAvg() != null) {
            computedRatingAvg = snapshotOpt.get().getRatingAvg();
            computedReviewCount = snapshotOpt.get().getReviewCount() != null ? snapshotOpt.get().getReviewCount() : 0;
        }

        if (snapshotOpt.isPresent()) {
            LandlordSnapshot ls = snapshotOpt.get();
            return HostPublicProfileDto.builder()
                    .id(ls.getId() != null ? ls.getId() : landlordId)
                    .landlordId(ls.getLandlordId() != null ? ls.getLandlordId() : landlordId)
                    .name(ls.getName())
                    .avatarUrl(ls.getAvatarUrl() != null && !ls.getAvatarUrl().isBlank() ? ls.getAvatarUrl() : "/uploads/avatars/default.png")
                    .verified(ls.isVerified())
                    .ratingAvg(computedRatingAvg)
                    .reviewCount(computedReviewCount)
                    .responseRatePercent(ls.getResponseRatePercent() != null ? ls.getResponseRatePercent() : 100)
                    .responseTimeMinutes(ls.getResponseTimeMinutes() != null ? ls.getResponseTimeMinutes() : 15)
                    .roomCount(roomDtos.size())
                    .trustScore(ls.getTrustScore() != null ? ls.getTrustScore() : 100)
                    .joinedAt(ls.getJoinedAt())
                    .rooms(roomDtos)
                    .build();
        }

        Room first = publicRooms.get(0);
        return HostPublicProfileDto.builder()
                .id(landlordId)
                .landlordId(landlordId)
                .name(first.getLandlordName())
                .avatarUrl("/uploads/avatars/default.png")
                .verified(first.isVerifiedOwnership())
                .ratingAvg(computedRatingAvg)
                .reviewCount(computedReviewCount)
                .responseRatePercent(100)
                .responseTimeMinutes(15)
                .roomCount(roomDtos.size())
                .trustScore(100)
                .rooms(roomDtos)
                .build();
    }

    public List<HostPublicProfileDto> getTopHosts() {
        List<LandlordSnapshot> snapshots = landlordSnapshotRepository.findAll().stream()
                .sorted(Comparator.comparing(LandlordSnapshot::getRatingAvg, Comparator.reverseOrder()))
                .toList();
        return snapshots.stream().map(ls -> {
            int roomCount = ls.getRoomCount() > 0 ? ls.getRoomCount() : 1;
            String avatar = (ls.getAvatarUrl() != null && !ls.getAvatarUrl().isBlank())
                    ? ls.getAvatarUrl()
                    : "/uploads/avatars/default.png";
            return HostPublicProfileDto.builder()
                    .id(ls.getId() != null ? ls.getId() : ls.getLandlordId())
                    .landlordId(ls.getLandlordId())
                    .name(ls.getName())
                    .avatarUrl(avatar)
                    .verified(ls.isVerified())
                    .ratingAvg(ls.getRatingAvg())
                    .reviewCount(ls.getReviewCount())
                    .responseRatePercent(ls.getResponseRatePercent())
                    .responseTimeMinutes(ls.getResponseTimeMinutes())
                    .roomCount(roomCount)
                    .trustScore(ls.getTrustScore() != null ? ls.getTrustScore() : 85)
                    .joinedAt(ls.getJoinedAt())
                    .rooms(List.of())
                    .build();
        }).toList();
    }

    private boolean isPublicRoom(Room r) {
        if (r.isDeleted() || r.isHidden() || !r.isApproved()) {
            return false;
        }
        return "PUBLISHED".equalsIgnoreCase(r.getPublicationStatus())
                || "PUBLISHED".equalsIgnoreCase(r.getStatus())
                || "AVAILABLE".equalsIgnoreCase(r.getStatus());
    }
}

