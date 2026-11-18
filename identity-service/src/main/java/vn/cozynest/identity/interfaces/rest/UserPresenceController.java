package vn.cozynest.identity.interfaces.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.identity.application.dto.UserPresenceDto;
import vn.cozynest.identity.application.service.UserPresenceService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserPresenceController {

    private final UserPresenceService userPresenceService;

    @GetMapping("/{userId}/presence")
    public ResponseEntity<ApiResponse<UserPresenceDto>> getUserPresence(@PathVariable String userId) {
        UserPresenceDto dto = userPresenceService.getPresence(userId);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<ApiResponse<Void>> recordHeartbeat(
            Authentication authentication,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String userId = null;
        if (authentication != null && authentication.getPrincipal() instanceof String principal && !"anonymousUser".equals(principal)) {
            userId = principal;
        } else if (body != null && body.containsKey("userId")) {
            userId = body.get("userId");
        }
        if (userId != null && !userId.isBlank()) {
            userPresenceService.recordHeartbeat(userId);
        }
        return ResponseEntity.ok(ApiResponse.message("Heartbeat received"));
    }

    @PostMapping("/{userId}/heartbeat")
    public ResponseEntity<ApiResponse<Void>> recordHeartbeatForUser(@PathVariable String userId) {
        userPresenceService.recordHeartbeat(userId);
        return ResponseEntity.ok(ApiResponse.message("Heartbeat received"));
    }

    @PostMapping("/offline")
    public ResponseEntity<ApiResponse<Void>> recordOffline(
            Authentication authentication,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String userId = null;
        if (authentication != null && authentication.getPrincipal() instanceof String principal && !"anonymousUser".equals(principal)) {
            userId = principal;
        } else if (body != null && body.containsKey("userId")) {
            userId = body.get("userId");
        }
        if (userId != null && !userId.isBlank()) {
            userPresenceService.recordOffline(userId);
        }
        return ResponseEntity.ok(ApiResponse.message("User marked offline"));
    }

    @PostMapping("/{userId}/offline")
    public ResponseEntity<ApiResponse<Void>> recordOfflineForUser(@PathVariable String userId) {
        userPresenceService.recordOffline(userId);
        return ResponseEntity.ok(ApiResponse.message("User marked offline"));
    }
}
