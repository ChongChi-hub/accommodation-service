package vn.toam.listing.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.toam.common.dto.ApiResponse;
import vn.toam.common.dto.PaginationResponse;
import vn.toam.listing.application.dto.*;
import vn.toam.listing.application.service.RoomQueryService;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
@Tag(name = "Room Listing", description = "Tìm kiếm và xem chi tiết phòng trọ")
public class RoomController {

    private final RoomQueryService roomQueryService;

    @GetMapping
    @Operation(summary = "Tìm kiếm danh sách phòng trọ theo bộ lọc")
    public ResponseEntity<ApiResponse<List<RoomDto>>> searchRooms(
            @RequestParam(required = false) String provinceCode,
            @RequestParam(required = false) String districtCode,
            @RequestParam(required = false) String roomTypeCode,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Double minArea,
            @RequestParam(required = false) Double maxArea,
            @RequestParam(required = false) Integer occupants,
            @RequestParam(required = false) Boolean allowPets,
            @RequestParam(required = false) Boolean allowCooking,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "12") Integer size
    ) {
        RoomFilterParams params = RoomFilterParams.builder()
                .provinceCode(provinceCode)
                .districtCode(districtCode)
                .roomTypeCode(roomTypeCode)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .minArea(minArea)
                .maxArea(maxArea)
                .occupants(occupants)
                .allowPets(allowPets)
                .allowCooking(allowCooking)
                .sortBy(sortBy)
                .page(page)
                .size(size)
                .build();

        Page<RoomDto> result = roomQueryService.searchRooms(params);

        PaginationResponse pagination = PaginationResponse.of(
                result.getNumber() + 1,
                result.getSize(),
                result.getTotalElements()
        );

        ApiResponse<List<RoomDto>> response = ApiResponse.<List<RoomDto>>builder()
                .message("Lấy danh sách phòng trọ thành công")
                .data(result.getContent())
                .pagination(pagination)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết một phòng trọ")
    public ResponseEntity<ApiResponse<RoomDto>> getRoomById(@PathVariable Long id) {
        RoomDto room = roomQueryService.getRoomById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin phòng thành công", room));
    }

    @GetMapping("/{id}/recommendations")
    @Operation(summary = "Gợi ý phòng tương tự cùng khu vực")
    public ResponseEntity<ApiResponse<List<RoomDto>>> getRecommendations(@PathVariable Long id) {
        List<RoomDto> rooms = roomQueryService.getRecommendations(id);
        return ResponseEntity.ok(ApiResponse.success(rooms));
    }
}
