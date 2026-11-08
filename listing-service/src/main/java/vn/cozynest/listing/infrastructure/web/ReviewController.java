package vn.cozynest.listing.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.listing.application.dto.ReviewDto;
import vn.cozynest.listing.application.service.RoomQueryService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Đánh giá phòng trọ từ người thuê")
public class ReviewController {

    private final RoomQueryService roomQueryService;

    @GetMapping
    @Operation(summary = "Lấy danh sách đánh giá theo roomId")
    public ResponseEntity<ApiResponse<List<ReviewDto>>> getReviews(@RequestParam Long roomId) {
        return ResponseEntity.ok(ApiResponse.success(roomQueryService.getReviewsByRoom(roomId)));
    }
}

