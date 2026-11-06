package com.service.room.controller;

import com.service.common.dto.ApiResponse;
import com.service.room.dto.CreateRoomRequest;
import com.service.room.dto.UpdateRoomStatusRequest;
import com.service.room.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listRooms(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String roomTypeId,
            @RequestParam(required = false) Double priceMin,
            @RequestParam(required = false) Double priceMax,
            @RequestParam(required = false) Double areaMin,
            @RequestParam(required = false) Double areaMax,
            @RequestParam(required = false) String wardId,
            @RequestParam(required = false) String districtId,
            @RequestParam(required = false) String provinceId,
            @RequestParam(required = false) Integer maxOccupants,
            @RequestParam(required = false) String allowPet,
            @RequestParam(required = false) String allowCooking,
            @RequestParam(required = false) String amenities,
            @RequestParam(defaultValue = "newest") String sortBy
    ) {
        Map<String, Object> response = roomService.listRooms(
                page, limit, search, roomTypeId, priceMin, priceMax,
                areaMin, areaMax, wardId, districtId, provinceId,
                maxOccupants, allowPet, allowCooking, amenities, sortBy
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-rooms")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyRooms(Authentication authentication) {
        String landlordId = (String) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(roomService.getMyRooms(landlordId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRoomById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(roomService.getRoomById(id)));
    }

    @GetMapping("/{id}/recommendations")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRecommendations(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(roomService.getRecommendations(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> createRoom(
            Authentication authentication,
            @Valid @RequestBody CreateRoomRequest request
    ) {
        String userId = (String) authentication.getPrincipal();
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", "").toLowerCase())
                .orElse("tenant");

        String roomId = roomService.createRoom(userId, role, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đăng phòng thành công", Map.of("id", roomId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateRoom(
            Authentication authentication,
            @PathVariable String id,
            @RequestBody CreateRoomRequest request
    ) {
        String userId = (String) authentication.getPrincipal();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        roomService.updateRoom(id, userId, isAdmin, request);
        return ResponseEntity.ok(ApiResponse.message("Cập nhật phòng thành công"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(
            Authentication authentication,
            @PathVariable String id
    ) {
        String userId = (String) authentication.getPrincipal();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        roomService.deleteRoom(id, userId, isAdmin);
        return ResponseEntity.ok(ApiResponse.message("Xoá phòng thành công"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateRoomStatus(
            Authentication authentication,
            @PathVariable String id,
            @Valid @RequestBody UpdateRoomStatusRequest request
    ) {
        String userId = (String) authentication.getPrincipal();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        roomService.updateRoomStatus(id, userId, isAdmin, request.getStatus());
        return ResponseEntity.ok(ApiResponse.message("Cập nhật trạng thái phòng thành công"));
    }
}

