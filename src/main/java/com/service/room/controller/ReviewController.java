package com.service.room.controller;

import com.service.common.dto.ApiResponse;
import com.service.room.dto.CreateReviewRequest;
import com.service.room.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> createReview(
            Authentication authentication,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        String userId = (String) authentication.getPrincipal();
        String reviewId = reviewService.createReview(userId, request.getRoomId(), request.getRating(), request.getComment());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đánh giá thành công", Map.of("id", reviewId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listReviews(@RequestParam String roomId) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.listReviews(roomId)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            Authentication authentication,
            @PathVariable String id
    ) {
        String userId = (String) authentication.getPrincipal();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        reviewService.deleteReview(id, userId, isAdmin);
        return ResponseEntity.ok(ApiResponse.message("Xoá đánh giá thành công"));
    }
}

