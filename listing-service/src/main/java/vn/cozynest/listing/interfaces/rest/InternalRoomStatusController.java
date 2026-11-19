package vn.cozynest.listing.interfaces.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.common.dto.ApiResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Internal service-to-service endpoint for updating room rental/occupancy status
 * when a contract is activated (RENTED/OCCUPIED) or terminated/cancelled (PUBLISHED/AVAILABLE).
 */
@RestController
@RequestMapping("/internal/rooms")
public class InternalRoomStatusController {

    private static final String DEFAULT_INTERNAL_KEY = "CozyNestInternalServiceKeySecret2026";
    private static final Set<String> ALLOWED_STATUSES = Set.of("RENTED", "AVAILABLE", "PUBLISHED", "RESERVED");
    private static final Set<String> ALLOWED_OCCUPANCY = Set.of("OCCUPIED", "AVAILABLE", "RESERVED");

    private final JdbcTemplate jdbcTemplate;
    private final String expectedServiceKey;

    @Autowired
    public InternalRoomStatusController(
            JdbcTemplate jdbcTemplate,
            @Value("${app.internal.service-key:" + DEFAULT_INTERNAL_KEY + "}") String expectedServiceKey,
            @Autowired(required = false) Environment env
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.expectedServiceKey = expectedServiceKey;
        if (env != null && env.matchesProfiles("prod", "production", "staging")) {
            if (DEFAULT_INTERNAL_KEY.equals(expectedServiceKey)) {
                throw new IllegalStateException("Restricted environment requires explicit app.internal.service-key");
            }
        }
    }

    public record RoomStatusUpdateRequest(String status, String occupancyStatus, String reason) {}

    @PostMapping("/{roomId}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateRoomStatus(
            @PathVariable String roomId,
            @RequestBody RoomStatusUpdateRequest request,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String serviceKey
    ) {
        if (serviceKey == null || !isKeyValid(serviceKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Truy cap noi bo khong hop le", "ERR_UNAUTHORIZED"));
        }
        if (roomId == null || !roomId.matches("\\d+")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Khong tim thay phong: " + roomId, "ERR_NOT_FOUND"));
        }
        String nextStatus = request != null && request.status() != null
                ? request.status().trim().toUpperCase() : "";
        String nextOccupancy = request != null && request.occupancyStatus() != null
                ? request.occupancyStatus().trim().toUpperCase()
                : ("RENTED".equals(nextStatus) ? "OCCUPIED" : "AVAILABLE");
        if (!ALLOWED_STATUSES.contains(nextStatus) || !ALLOWED_OCCUPANCY.contains(nextOccupancy)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Trang thai phong khong hop le: " + nextStatus, "ERR_BAD_REQUEST"));
        }

        long id = Long.parseLong(roomId);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, status, occupancy_status, total_spots FROM rooms WHERE id = ? AND deleted_at IS NULL",
                id
        );
        if (rows.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Khong tim thay phong: " + roomId, "ERR_NOT_FOUND"));
        }

        Map<String, Object> existing = rows.get(0);
        String currentStatus = String.valueOf(existing.get("status"));
        String currentOccupancy = String.valueOf(existing.get("occupancy_status"));
        if (nextStatus.equals(currentStatus) && nextOccupancy.equals(currentOccupancy)) {
            return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "roomId", roomId,
                    "status", currentStatus,
                    "occupancyStatus", currentOccupancy,
                    "idempotent", true
            )));
        }

        Integer totalSpots = existing.get("total_spots") instanceof Number n ? n.intValue() : 1;
        int nextAvailableSpots = "RENTED".equals(nextStatus) || "OCCUPIED".equals(nextOccupancy)
                ? 0 : Math.max(1, totalSpots);

        jdbcTemplate.update(
                "UPDATE rooms SET status = ?, occupancy_status = ?, available_spots = ?, updated_at = NOW() WHERE id = ?",
                nextStatus, nextOccupancy, nextAvailableSpots, id
        );

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "roomId", roomId,
                "status", nextStatus,
                "occupancyStatus", nextOccupancy,
                "idempotent", false
        )));
    }

    private boolean isKeyValid(String serviceKey) {
        if (expectedServiceKey == null) return false;
        return MessageDigest.isEqual(
                serviceKey.getBytes(StandardCharsets.UTF_8),
                expectedServiceKey.getBytes(StandardCharsets.UTF_8)
        );
    }
}

