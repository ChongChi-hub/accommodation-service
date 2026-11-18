package vn.cozynest.listing.infrastructure.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.listing.infrastructure.persistence.entity.AmenityJpaEntity;
import vn.cozynest.listing.infrastructure.persistence.entity.LandlordSnapshotJpaEntity;
import vn.cozynest.listing.infrastructure.persistence.entity.ReviewJpaEntity;
import vn.cozynest.listing.infrastructure.persistence.entity.RoomImageJpaEntity;
import vn.cozynest.listing.infrastructure.persistence.entity.RoomJpaEntity;
import vn.cozynest.listing.infrastructure.persistence.entity.RoomStatusHistoryJpaEntity;
import vn.cozynest.listing.infrastructure.persistence.repository.SpringDataLandlordSnapshotRepository;
import vn.cozynest.listing.infrastructure.persistence.repository.SpringDataReviewRepository;
import vn.cozynest.listing.infrastructure.persistence.repository.SpringDataRoomRepository;
import vn.cozynest.listing.infrastructure.persistence.repository.SpringDataRoomStatusHistoryRepository;
import vn.cozynest.listing.infrastructure.security.LandlordContextResolver;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminListingController {

    private final SpringDataRoomRepository roomRepository;
    private final SpringDataReviewRepository reviewRepository;
    private final SpringDataRoomStatusHistoryRepository roomStatusHistoryRepository;
    private final SpringDataLandlordSnapshotRepository landlordSnapshotRepository;
    private final LandlordContextResolver contextResolver;
    private final Optional<vn.cozynest.listing.infrastructure.client.ListingNotificationClient> notificationClient;

    public record AdminRoomDto(
            Long id,
            String roomUuid,
            String title,
            String landlordName,
            String landlordPhone,
            Long landlordId,
            java.math.BigDecimal price,
            java.math.BigDecimal deposit,
            java.math.BigDecimal electricityCost,
            java.math.BigDecimal waterCost,
            java.math.BigDecimal internetCost,
            java.math.BigDecimal serviceCost,
            double area,
            String address,
            String provinceCode,
            String districtCode,
            String wardCode,
            String description,
            String roomTypeCode,
            Integer floor,
            Integer bedrooms,
            Integer bathrooms,
            Integer maxOccupants,
            Boolean allowPets,
            Boolean allowCooking,
            String curfew,
            String houseRules,
            List<String> images,
            List<String> amenities,
            String submittedAt,
            String status,
            String riskLevel,
            String riskReasons,
            Boolean verifiedOwnership
    ) {}

    public record AdminReviewDto(
            Long id,
            String tenantName,
            String roomTitle,
            int rating,
            String comment,
            String status
    ) {}

    private AdminRoomDto toAdminRoomDto(RoomJpaEntity r) {
        String st = "APPROVED";
        if ("REJECTED".equalsIgnoreCase(r.getStatus())) {
            st = "REJECTED";
        } else if ("PENDING_REVIEW".equalsIgnoreCase(r.getStatus()) || "PENDING".equalsIgnoreCase(r.getStatus()) || !r.isApproved()) {
            st = "PENDING";
        }
        String submitted = r.getCreatedAt() != null ? r.getCreatedAt().toString() : "";
        String resolvedLandlordName = r.getLandlordName();
        if (resolvedLandlordName == null || resolvedLandlordName.isBlank() || resolvedLandlordName.matches("(?i).*\\d{5,}.*")) {
            resolvedLandlordName = landlordSnapshotRepository.findByLandlordId(r.getLandlordId())
                    .map(LandlordSnapshotJpaEntity::getName)
                    .filter(n -> n != null && !n.isBlank())
                    .orElse(resolvedLandlordName != null && !resolvedLandlordName.isBlank() ? resolvedLandlordName : "Chủ trọ");
        }
        String resolvedLandlordPhone = r.getLandlordPhone();
        if (resolvedLandlordPhone == null || resolvedLandlordPhone.isBlank()) {
            resolvedLandlordPhone = landlordSnapshotRepository.findByLandlordId(r.getLandlordId())
                    .map(LandlordSnapshotJpaEntity::getPhone)
                    .filter(p -> p != null && !p.isBlank())
                    .orElse(null);
        }

        List<String> images = r.getImages() != null ? r.getImages().stream()
                .sorted(Comparator.comparing(RoomImageJpaEntity::isCover).reversed()
                        .thenComparing(RoomImageJpaEntity::isPrimary).reversed()
                        .thenComparing(img -> img.getSortOrder() != null ? img.getSortOrder() : 0)
                        .thenComparing(img -> img.getDisplayOrder() != null ? img.getDisplayOrder() : 0))
                .map(RoomImageJpaEntity::getImageUrl)
                .filter(url -> url != null && !url.isBlank())
                .toList() : List.of();

        List<String> amenities = r.getAmenities() != null ? r.getAmenities().stream()
                .map(AmenityJpaEntity::getName)
                .filter(name -> name != null && !name.isBlank())
                .toList() : List.of();

        return new AdminRoomDto(
                r.getId(),
                r.getRoomUuid(),
                r.getTitle(),
                resolvedLandlordName,
                resolvedLandlordPhone,
                r.getLandlordId(),
                r.getPrice(),
                r.getDeposit(),
                r.getElectricityCost(),
                r.getWaterCost(),
                r.getInternetCost(),
                r.getServiceCost(),
                r.getArea(),
                r.getAddress(),
                r.getProvinceCode(),
                r.getDistrictCode(),
                r.getWardCode(),
                r.getDescription(),
                r.getRoomTypeCode(),
                r.getFloor(),
                r.getBedrooms(),
                r.getBathrooms(),
                r.getMaxOccupants(),
                r.isAllowPets(),
                r.isAllowCooking(),
                r.getCurfew(),
                r.getHouseRules(),
                images,
                amenities,
                submitted,
                st,
                r.getRiskLevel() != null ? r.getRiskLevel() : "NORMAL",
                r.getRiskReasons(),
                r.isVerifiedOwnership()
        );
    }

    @GetMapping("/rooms")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<AdminRoomDto>>> getAdminRooms() {
        List<RoomJpaEntity> all = roomRepository.findAll();
        List<AdminRoomDto> list = all.stream()
                .filter(r -> r.getStatus() != null && !"DRAFT".equalsIgnoreCase(r.getStatus()))
                .map(this::toAdminRoomDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/rooms/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<AdminRoomDto>> getAdminRoomById(@PathVariable Long id) {
        RoomJpaEntity room = roomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay phong: " + id));
        return ResponseEntity.ok(ApiResponse.success(toAdminRoomDto(room)));
    }

    @PostMapping("/rooms/{id}/approve")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> approveRoom(@PathVariable Long id) {
        RoomJpaEntity room = roomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay phong: " + id));

        boolean isAdmin = contextResolver != null && contextResolver.isAdmin();
        boolean isModerator = contextResolver != null && contextResolver.isModerator();
        String actorId = contextResolver != null ? contextResolver.getCurrentActorId() : "system";
        String actorRole = isAdmin ? "ADMIN" : (isModerator ? "MODERATOR" : "ADMIN");

        if (room.isApproved() && "AVAILABLE".equalsIgnoreCase(room.getStatus())) {
            return ResponseEntity.ok(ApiResponse.message("Tin dang phong tro da duoc phe duyet truoc do."));
        }

        if ("REJECTED".equalsIgnoreCase(room.getStatus()) && !isAdmin) {
            throw new IllegalStateException("Tin dang da bi tu choi truoc do. Chi Quan tri vien (Admin) moi co quyen phe duyet lai.");
        }

        String fromStatus = room.getStatus();
        room.setApproved(true);
        room.setStatus("AVAILABLE");
        room.setPublicationStatus("PUBLISHED");
        room.setOccupancyStatus("AVAILABLE");
        room.setRiskLevel("NORMAL");
        room.setHiddenBy(actorRole);
        room.setUpdatedAt(LocalDateTime.now());
        roomRepository.save(room);

        if (roomStatusHistoryRepository != null) {
            roomStatusHistoryRepository.save(RoomStatusHistoryJpaEntity.builder()
                    .roomId(room.getId())
                    .fromStatus(fromStatus)
                    .toStatus("AVAILABLE")
                    .actorId(actorId)
                    .actorRole(actorRole)
                    .reason(isAdmin ? "Quan tri vien (Admin) phe duyet bai dang" : "Kiem duyet vien (Moderator) phe duyet bai dang")
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        if (notificationClient != null && notificationClient.isPresent()) {
            String targetUser = resolveLandlordUserId(room.getLandlordId());
            notificationClient.get().sendNotification(
                    targetUser,
                    "ROOM",
                    "Tin đăng phòng trọ đã được phê duyệt",
                    "Tin đăng \"" + room.getTitle() + "\" của bạn đã được " + (isAdmin ? "Quản trị viên" : "Kiểm duyệt viên") + " phê duyệt thành công và hiển thị công khai trên CozyNest.",
                    "/dashboard/landlord/rooms"
            );
        }

        return ResponseEntity.ok(ApiResponse.message("Phe duyet phong thanh cong"));
    }

    @PostMapping("/rooms/{id}/reject")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> rejectRoom(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        RoomJpaEntity room = roomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay phong: " + id));

        boolean isAdmin = contextResolver != null && contextResolver.isAdmin();
        boolean isModerator = contextResolver != null && contextResolver.isModerator();
        String actorId = contextResolver != null ? contextResolver.getCurrentActorId() : "system";
        String actorRole = isAdmin ? "ADMIN" : (isModerator ? "MODERATOR" : "ADMIN");

        // Moderator CANNOT reject an already approved room!
        if (room.isApproved() || "AVAILABLE".equalsIgnoreCase(room.getStatus()) || "PUBLISHED".equalsIgnoreCase(room.getPublicationStatus())) {
            if (!isAdmin) {
                throw new IllegalStateException("Tin đăng phòng trọ này đã được Quản trị viên (Admin) phê duyệt. Kiểm duyệt viên không có quyền từ chối bài đăng đã được Admin chấp thuận.");
            }
        }

        if ("REJECTED".equalsIgnoreCase(room.getStatus())) {
            return ResponseEntity.ok(ApiResponse.message("Phong tro da o trang thai tu choi truoc do."));
        }

        boolean isPending = !room.isApproved() && ("PENDING_REVIEW".equalsIgnoreCase(room.getStatus()) || "PENDING".equalsIgnoreCase(room.getStatus()) || "DRAFT".equalsIgnoreCase(room.getStatus()));
        if (!isPending && !isAdmin) {
            throw new IllegalStateException("Bài đăng không ở trạng thái chờ duyệt. Kiểm duyệt viên không thể thao tác.");
        }

        String fromStatus = room.getStatus();
        String reason = body != null ? body.getOrDefault("reason", "Vi pham quy che dang tin") : "Vi pham quy che";
        room.setApproved(false);
        room.setStatus("REJECTED");
        room.setPublicationStatus("REJECTED");
        room.setRejectedReason(reason);
        room.setHiddenBy(actorRole);
        room.setUpdatedAt(LocalDateTime.now());
        roomRepository.save(room);

        if (roomStatusHistoryRepository != null) {
            roomStatusHistoryRepository.save(RoomStatusHistoryJpaEntity.builder()
                    .roomId(room.getId())
                    .fromStatus(fromStatus)
                    .toStatus("REJECTED")
                    .actorId(actorId)
                    .actorRole(actorRole)
                    .reason(reason)
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        if (notificationClient != null && notificationClient.isPresent()) {
            String targetUser = resolveLandlordUserId(room.getLandlordId());
            notificationClient.get().sendNotification(
                    targetUser,
                    "ROOM",
                    "Tin đăng phòng trọ bị từ chối",
                    "Tin đăng \"" + room.getTitle() + "\" chưa được phê duyệt: " + reason + ".",
                    "/dashboard/landlord/rooms"
            );
        }

        return ResponseEntity.ok(ApiResponse.message("Tu choi phong thanh cong"));
    }

    private String resolveLandlordUserId(Long landlordId) {
        if (landlordId == null) return "u-huy-01";
        if (landlordId == 1L) return "u-huy-01";
        if (landlordId == 2L) return "u-hao-01";
        return "u-huy-01";
    }

    @GetMapping("/reviews")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<AdminReviewDto>>> getAdminReviews() {
        List<ReviewJpaEntity> reviews = reviewRepository.findAll();
        List<AdminReviewDto> list = reviews.stream().map(rev -> {
            String roomTitle = roomRepository.findById(rev.getRoomId())
                    .map(RoomJpaEntity::getTitle)
                    .orElse("Phòng trọ #" + rev.getRoomId());
            String status = rev.getStatus() != null ? rev.getStatus() : "VISIBLE";
            return new AdminReviewDto(
                    rev.getId(),
                    rev.getTenantName(),
                    roomTitle,
                    rev.getRating(),
                    rev.getComment() != null ? rev.getComment() : "",
                    status
            );
        }).toList();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping("/reviews/{id}/toggle")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleReview(@PathVariable Long id) {
        ReviewJpaEntity review = reviewRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đánh giá: " + id));
        String current = review.getStatus() != null ? review.getStatus() : "VISIBLE";
        String next = "VISIBLE".equalsIgnoreCase(current) ? "HIDDEN" : "VISIBLE";
        review.setStatus(next);
        reviewRepository.save(review);

        roomRepository.findById(review.getRoomId()).ifPresent(room -> {
            List<ReviewJpaEntity> roomReviews = reviewRepository.findByRoomIdInOrderByCreatedAtDesc(List.of(room.getId()))
                    .stream()
                    .filter(r -> r.getStatus() == null || "VISIBLE".equalsIgnoreCase(r.getStatus()))
                    .toList();
            int count = roomReviews.size();
            double avg = roomReviews.isEmpty() ? 0.0 : roomReviews.stream().mapToInt(ReviewJpaEntity::getRating).average().orElse(0.0);
            room.setReviewCount(count);
            room.setRatingAvg(Math.round(avg * 10.0) / 10.0);
            roomRepository.save(room);

            updateLandlordRatings(room.getLandlordId());
        });

        return ResponseEntity.ok(ApiResponse.success(Map.of("id", id, "status", next)));
    }

    private void updateLandlordRatings(Long landlordId) {
        if (landlordId == null) return;
        List<RoomJpaEntity> landlordRooms = roomRepository.findByLandlordId(landlordId);
        if (landlordRooms.isEmpty()) return;

        List<Long> roomIds = landlordRooms.stream().map(RoomJpaEntity::getId).toList();
        List<ReviewJpaEntity> visibleReviews = reviewRepository.findByRoomIdInOrderByCreatedAtDesc(roomIds).stream()
                .filter(r -> r.getStatus() == null || "VISIBLE".equalsIgnoreCase(r.getStatus()))
                .toList();

        int totalReviews = visibleReviews.size();
        double landlordAvg = totalReviews > 0
                ? visibleReviews.stream().mapToInt(ReviewJpaEntity::getRating).average().orElse(5.0)
                : 5.0;

        landlordSnapshotRepository.findByLandlordId(landlordId).ifPresent(ls -> {
            ls.setReviewCount(totalReviews);
            ls.setRatingAvg(Math.round(landlordAvg * 10.0) / 10.0);
            ls.setTrustScore(100);
            landlordSnapshotRepository.save(ls);
        });
    }
}

