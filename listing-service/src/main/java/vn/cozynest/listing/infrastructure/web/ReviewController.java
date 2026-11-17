package vn.cozynest.listing.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.listing.application.dto.ReviewDto;
import vn.cozynest.listing.application.service.RoomQueryService;
import vn.cozynest.listing.infrastructure.persistence.entity.ReviewJpaEntity;
import vn.cozynest.listing.infrastructure.persistence.entity.RoomJpaEntity;
import vn.cozynest.listing.infrastructure.persistence.repository.SpringDataReviewRepository;
import vn.cozynest.listing.infrastructure.persistence.repository.SpringDataRoomRepository;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Đánh giá phòng trọ từ người thuê")
public class ReviewController {

    private final RoomQueryService roomQueryService;
    private final SpringDataReviewRepository reviewRepository;
    private final SpringDataRoomRepository roomRepository;
    private final vn.cozynest.listing.infrastructure.security.LandlordContextResolver landlordContextResolver;

    private Long resolveTenantId(Long paramTenantId, Authentication authentication) {
        if (paramTenantId != null) {
            return paramTenantId;
        }
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        String principal = authentication.getName();
        if ("u-tenant-01".equals(principal) || "tenant@cozynest.vn".equals(principal)) {
            return 1L;
        }
        if ("u-tenant-02".equals(principal) || "tenant2@cozynest.vn".equals(principal)) {
            return 2L;
        }
        if ("u-tenant-03".equals(principal) || "tenant3@cozynest.vn".equals(principal)) {
            return 3L;
        }
        try {
            return Long.parseLong(principal);
        } catch (NumberFormatException ignored) {
            long hash = Math.abs((long) principal.hashCode());
            return hash > 100 ? hash : hash + 1000L;
        }
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách đánh giá theo roomId")
    public ResponseEntity<ApiResponse<List<ReviewDto>>> getReviews(@RequestParam Long roomId) {
        return ResponseEntity.ok(ApiResponse.success(roomQueryService.getReviewsByRoom(roomId)));
    }

    @GetMapping("/tenant")
    @Operation(summary = "Lấy danh sách đánh giá của người thuê")
    public ResponseEntity<ApiResponse<List<ReviewDto>>> getTenantReviews(
            @RequestParam(required = false) Long tenantId,
            Authentication authentication
    ) {
        Long resolvedTenantId = resolveTenantId(tenantId, authentication);
        if (resolvedTenantId == null) {
            return ResponseEntity.ok(ApiResponse.success(List.of()));
        }
        List<ReviewJpaEntity> reviews = reviewRepository.findByTenantIdOrderByCreatedAtDesc(resolvedTenantId);
        List<ReviewDto> dtos = reviews.stream().map(r -> {
            String roomTitle = roomRepository.findById(r.getRoomId())
                    .map(RoomJpaEntity::getTitle)
                    .orElse("Phòng trọ #" + r.getRoomId());
            return ReviewDto.builder()
                    .id(r.getId())
                    .roomId(r.getRoomId())
                    .roomTitle(roomTitle)
                    .tenantId(r.getTenantId())
                    .tenantName(r.getTenantName())
                    .tenantAvatar(r.getTenantAvatar())
                    .rating(r.getRating())
                    .comment(r.getComment())
                    .landlordReply(r.getLandlordReply())
                    .createdAt(r.getCreatedAt())
                    .build();
        }).toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/landlord")
    @Operation(summary = "Lấy danh sách đánh giá các phòng của chủ trọ")
    public ResponseEntity<ApiResponse<List<ReviewDto>>> getLandlordReviews(
            @RequestParam(required = false) Long landlordId,
            Authentication authentication
    ) {
        Long resolvedLandlordId = landlordId != null ? landlordId : landlordContextResolver.getCurrentLandlordId();
        if (resolvedLandlordId == null) {
            return ResponseEntity.ok(ApiResponse.success(List.of()));
        }
        List<RoomJpaEntity> rooms = roomRepository.findByLandlordId(resolvedLandlordId);
        if (rooms.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(List.of()));
        }
        List<Long> roomIds = rooms.stream().map(RoomJpaEntity::getId).toList();
        List<ReviewJpaEntity> reviews = reviewRepository.findByRoomIdInOrderByCreatedAtDesc(roomIds);
        List<ReviewDto> dtos = reviews.stream().map(r -> {
            String roomTitle = roomRepository.findById(r.getRoomId())
                    .map(RoomJpaEntity::getTitle)
                    .orElse("Phòng trọ #" + r.getRoomId());
            return ReviewDto.builder()
                    .id(r.getId())
                    .roomId(r.getRoomId())
                    .roomTitle(roomTitle)
                    .tenantId(r.getTenantId())
                    .tenantName(r.getTenantName())
                    .tenantAvatar(r.getTenantAvatar())
                    .rating(r.getRating())
                    .comment(r.getComment())
                    .landlordReply(r.getLandlordReply())
                    .createdAt(r.getCreatedAt())
                    .build();
        }).toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @PostMapping("/{id}/reply")
    @Operation(summary = "Chủ trọ phản hồi đánh giá")
    public ResponseEntity<ApiResponse<ReviewDto>> replyReview(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        ReviewJpaEntity review = reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá: " + id));
        String reply = body != null ? body.getOrDefault("reply", "") : "";
        review.setLandlordReply(reply);
        reviewRepository.save(review);
        String roomTitle = roomRepository.findById(review.getRoomId())
                .map(RoomJpaEntity::getTitle)
                .orElse("Phòng trọ #" + review.getRoomId());
        ReviewDto dto = ReviewDto.builder()
                .id(review.getId())
                .roomId(review.getRoomId())
                .roomTitle(roomTitle)
                .tenantId(review.getTenantId())
                .tenantName(review.getTenantName())
                .tenantAvatar(review.getTenantAvatar())
                .rating(review.getRating())
                .comment(review.getComment())
                .landlordReply(review.getLandlordReply())
                .createdAt(review.getCreatedAt())
                .build();
        return ResponseEntity.ok(ApiResponse.success(dto));
    }
}

