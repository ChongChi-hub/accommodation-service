package com.service.room.controller;

import com.service.common.dto.ApiResponse;
import com.service.room.entity.AmenityEntity;
import com.service.room.entity.RoomTypeEntity;
import com.service.room.service.AmenityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/amenities")
public class AmenityController {

    private final AmenityService amenityService;

    public AmenityController(AmenityService amenityService) {
        this.amenityService = amenityService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AmenityEntity>>> listAmenities() {
        return ResponseEntity.ok(ApiResponse.success(amenityService.listAmenities()));
    }

    @GetMapping("/room-types")
    public ResponseEntity<ApiResponse<List<RoomTypeEntity>>> listRoomTypes() {
        return ResponseEntity.ok(ApiResponse.success(amenityService.listRoomTypes()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> createAmenity(@RequestBody Map<String, String> body) {
        amenityService.createAmenity(body.get("name"), body.get("icon"));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.message("Tạo tiện nghi thành công"));
    }
}

