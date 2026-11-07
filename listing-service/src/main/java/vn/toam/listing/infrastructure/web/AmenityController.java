package vn.toam.listing.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.toam.common.dto.ApiResponse;
import vn.toam.listing.application.dto.AmenityDto;
import vn.toam.listing.application.service.RoomQueryService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/amenities")
@RequiredArgsConstructor
@Tag(name = "Amenities", description = "Danh mục tiện nghi phòng trọ")
public class AmenityController {

    private final RoomQueryService roomQueryService;

    @GetMapping
    @Operation(summary = "Lấy tất cả danh mục tiện ích phòng trọ")
    public ResponseEntity<ApiResponse<List<AmenityDto>>> getAllAmenities() {
        return ResponseEntity.ok(ApiResponse.success(roomQueryService.getAllAmenities()));
    }
}
