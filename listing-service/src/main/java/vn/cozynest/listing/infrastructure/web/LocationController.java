package vn.cozynest.listing.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.listing.application.dto.LocationDto.*;
import vn.cozynest.listing.application.service.LocationService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
@Tag(name = "Location Service", description = "Tỉnh thành, quận huyện, phường xã")
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/provinces")
    @Operation(summary = "Lấy danh sách tỉnh/thành phố")
    public ResponseEntity<ApiResponse<List<ProvinceDto>>> getProvinces() {
        return ResponseEntity.ok(ApiResponse.success(locationService.getProvinces()));
    }

    @GetMapping("/districts")
    @Operation(summary = "Lấy danh sách quận/huyện theo mã tỉnh")
    public ResponseEntity<ApiResponse<List<DistrictDto>>> getDistricts(@RequestParam String provinceCode) {
        return ResponseEntity.ok(ApiResponse.success(locationService.getDistricts(provinceCode)));
    }

    @GetMapping("/wards")
    @Operation(summary = "Lấy danh sách phường/xã theo tỉnh/thành (mô hình 2 cấp) hoặc quận/huyện (legacy)")
    public ResponseEntity<ApiResponse<List<WardDto>>> getWards(
            @RequestParam(required = false) String provinceCode,
            @RequestParam(required = false) String districtCode
    ) {
        if (provinceCode != null && !provinceCode.isBlank()) {
            return ResponseEntity.ok(ApiResponse.success(locationService.getWardsByProvince(provinceCode)));
        }
        if (districtCode != null && !districtCode.isBlank()) {
            return ResponseEntity.ok(ApiResponse.success(locationService.getWards(districtCode)));
        }
        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }
}

