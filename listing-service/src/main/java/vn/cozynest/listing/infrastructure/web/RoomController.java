package vn.cozynest.listing.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.common.dto.PaginationResponse;
import vn.cozynest.listing.application.dto.*;
import vn.cozynest.listing.application.service.HomeDataService;
import vn.cozynest.listing.application.service.RoomDetailService;
import vn.cozynest.listing.application.service.RoomQueryService;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
@Tag(name = "Room Listing", description = "Tìm kiếm và xem chi tiết phòng trọ")
public class RoomController {

    private final RoomQueryService roomQueryService;
    private final RoomDetailService roomDetailService;
    private final HomeDataService homeDataService;

    @GetMapping
    @Operation(summary = "Tìm kiếm danh sách phòng trọ theo bộ lọc")
    public ResponseEntity<ApiResponse<List<RoomDto>>> searchRooms(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String provinceCode,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String provinceSlug,
            @RequestParam(required = false) String districtCode,
            @RequestParam(required = false) String wardCode,
            @RequestParam(required = false) String roomTypeCode,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) BigDecimal priceMin,
            @RequestParam(required = false) BigDecimal priceMax,
            @RequestParam(required = false) Double minArea,
            @RequestParam(required = false) Double maxArea,
            @RequestParam(required = false) Double areaMin,
            @RequestParam(required = false) Double areaMax,
            @RequestParam(required = false) Integer occupants,
            @RequestParam(required = false) Boolean allowPets,
            @RequestParam(required = false) Boolean allowCooking,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "12") Integer size
    ) {
        String effectiveProvinceCode = (provinceCode != null && !provinceCode.isBlank()) ? provinceCode : null;
        String effectiveProvinceSlug = (provinceSlug != null && !provinceSlug.isBlank())
                ? provinceSlug
                : (effectiveProvinceCode == null && province != null && !province.isBlank() ? province : null);
        String effectiveRoomType = (roomTypeCode != null && !roomTypeCode.isBlank()) ? roomTypeCode : type;

        RoomFilterParams params = RoomFilterParams.builder()
                .q(q)
                .provinceCode(effectiveProvinceCode)
                .provinceSlug(effectiveProvinceSlug)
                .districtCode(districtCode)
                .wardCode(wardCode)
                .roomTypeCode(effectiveRoomType)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .priceMin(priceMin)
                .priceMax(priceMax)
                .minArea(minArea)
                .maxArea(maxArea)
                .areaMin(areaMin)
                .areaMax(areaMax)
                .occupants(occupants)
                .allowPets(allowPets)
                .allowCooking(allowCooking)
                .sort(sort)
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

    @GetMapping("/stats")
    @Operation(summary = "Lấy thống kê nền tảng cho trang chủ")
    public ResponseEntity<ApiResponse<PlatformStatsDto>> getPlatformStats() {
        return ResponseEntity.ok(ApiResponse.success(homeDataService.getPlatformStats()));
    }

    @GetMapping("/{slugOrId}")
    @Operation(summary = "Xem chi tiết một phòng trọ theo slug hoặc id")
    public ResponseEntity<ApiResponse<RoomDetailDto>> getRoomDetail(@PathVariable String slugOrId) {
        RoomDetailDto room = roomDetailService.getRoomDetail(slugOrId);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin phòng thành công", room));
    }

    @GetMapping("/{id}/similar")
    @Operation(summary = "Gợi ý phòng tương tự cùng khu vực và mức giá")
    public ResponseEntity<ApiResponse<List<RoomDto>>> getSimilarRooms(@PathVariable Long id) {
        List<RoomDto> rooms = roomDetailService.getSimilarRooms(id);
        return ResponseEntity.ok(ApiResponse.success(rooms));
    }

    @GetMapping("/{id}/recommendations")
    @Operation(summary = "Gợi ý phòng tương tự cùng khu vực")
    public ResponseEntity<ApiResponse<List<RoomDto>>> getRecommendations(@PathVariable Long id) {
        List<RoomDto> rooms = roomDetailService.getSimilarRooms(id);
        return ResponseEntity.ok(ApiResponse.success(rooms));
    }
}
