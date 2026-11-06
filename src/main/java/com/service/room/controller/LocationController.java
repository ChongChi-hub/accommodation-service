package com.service.room.controller;

import com.service.common.dto.ApiResponse;
import com.service.room.service.LocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/provinces")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getProvinces() {
        return ResponseEntity.ok(ApiResponse.success(locationService.getProvinces()));
    }

    @GetMapping("/districts")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDistricts(@RequestParam(required = false) String provinceId) {
        return ResponseEntity.ok(ApiResponse.success(locationService.getDistricts(provinceId)));
    }

    @GetMapping("/wards")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getWards(@RequestParam(required = false) String districtId) {
        return ResponseEntity.ok(ApiResponse.success(locationService.getWards(districtId)));
    }

    @PostMapping("/seed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> seedLocations() {
        locationService.seedLocations();
        return ResponseEntity.ok(ApiResponse.message("Seed dữ liệu địa giới hành chính thành công!"));
    }
}

