package vn.cozynest.listing.infrastructure.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.listing.infrastructure.persistence.entity.ReviewJpaEntity;
import vn.cozynest.listing.infrastructure.persistence.entity.RoomJpaEntity;
import vn.cozynest.listing.infrastructure.persistence.entity.RoomStatusHistoryJpaEntity;
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
    private final LandlordContextResolver contextResolver;
    private final Optional<vn.cozynest.listing.infrastructure.client.ListingNotificationClient> notificationClient;

    public record AdminRoomDto(
            Long id,
            String title,
            String landlordName,
            java.math.BigDecimal price,
            double area,
            String address,
            String submittedAt,
            String status,
            String riskLevel,
            String riskReasons
    ) {}

    public record AdminReviewDto(
            Long id,
            String tenantName,
            String roomTitle,
            int rating,
            String comment,
            String status
    ) {}

    @GetMapping("/rooms")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<AdminRoomDto>>> getAdminRooms() {
        List<RoomJpaEntity> all = roomRepository.findAll();
        List<AdminRoomDto> list = all.stream().map(r -> {
            String st = "APPROVED";
            if ("REJECTED".equalsIgnoreCase(r.getStatus())) {
                st = "REJECTED";
            } else if ("PENDING_REVIEW".equalsIgnoreCase(r.getStatus()) || "PENDING".equalsIgnoreCase(r.getStatus()) || !r.isApproved()) {
                st = "PENDING";
            }
            String submitted = r.getCreatedAt() != null ? r.getCreatedAt().toString() : "";
            return new AdminRoomDto(
                    r.getId(),
                    r.getTitle(),
                    r.getLandlordName() != null ? r.getLandlordName() : "Chủ trọ",
                    r.getPrice(),
                    r.getArea(),
                    r.getAddress(),
                    submitted,
                    st,
                    r.getRiskLevel() != null ? r.getRiskLevel() : "NORMAL",
                    r.getRiskReasons()
            );
        }).toList();
        return ResponseEntity.ok(ApiResponse.success(list));
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
        List<AdminReviewDto> list = reviews.stream().map(rev -> new AdminReviewDto(
                rev.getId(),
                rev.getTenantName(),
                "Phòng trọ #" + rev.getRoomId(),
                rev.getRating(),
                rev.getComment() != null ? rev.getComment() : "",
                "VISIBLE"
        )).toList();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping("/reviews/{id}/toggle")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> toggleReview(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.message("Da thay doi trang thai danh gia"));
    }
}

