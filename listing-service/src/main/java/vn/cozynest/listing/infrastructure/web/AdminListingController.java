package vn.cozynest.listing.infrastructure.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.listing.infrastructure.persistence.entity.ReviewJpaEntity;
import vn.cozynest.listing.infrastructure.persistence.entity.RoomJpaEntity;
import vn.cozynest.listing.infrastructure.persistence.repository.SpringDataReviewRepository;
import vn.cozynest.listing.infrastructure.persistence.repository.SpringDataRoomRepository;

import java.util.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminListingController {

    private final SpringDataRoomRepository roomRepository;
    private final SpringDataReviewRepository reviewRepository;

    public record AdminRoomDto(
            Long id,
            String title,
            String landlordName,
            java.math.BigDecimal price,
            double area,
            String address,
            String submittedAt,
            String status
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
            if (!r.isApproved()) {
                st = "REJECTED".equalsIgnoreCase(r.getStatus()) ? "REJECTED" : "PENDING";
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
                    st
            );
        }).toList();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping("/rooms/{id}/approve")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> approveRoom(@PathVariable Long id) {
        RoomJpaEntity room = roomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay phong: " + id));
        room.setApproved(true);
        room.setStatus("AVAILABLE");
        room.setPublicationStatus("PUBLISHED");
        room.setOccupancyStatus("AVAILABLE");
        roomRepository.save(room);
        return ResponseEntity.ok(ApiResponse.message("Phe duyet phong thanh cong"));
    }

    @PostMapping("/rooms/{id}/reject")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> rejectRoom(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        RoomJpaEntity room = roomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay phong: " + id));
        String reason = body != null ? body.getOrDefault("reason", "Vi pham quy che dang tin") : "Vi pham quy che";
        room.setApproved(false);
        room.setStatus("REJECTED");
        room.setPublicationStatus("REJECTED");
        room.setRejectedReason(reason);
        roomRepository.save(room);
        return ResponseEntity.ok(ApiResponse.message("Tu choi phong thanh cong"));
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

