package vn.cozynest.communication.interfaces.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.communication.application.dto.NotificationDto;
import vn.cozynest.communication.application.dto.UnreadCountDto;
import vn.cozynest.communication.application.service.NotificationApplicationService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationApplicationService notificationApplicationService;

    public NotificationController(NotificationApplicationService notificationApplicationService) {
        this.notificationApplicationService = notificationApplicationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationDto>>> getNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "20") int limit
    ) {
        String userId = extractUserId(authentication);
        List<NotificationDto> items = notificationApplicationService.getNotifications(userId, limit);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountDto>> getUnreadCount(Authentication authentication) {
        String userId = extractUserId(authentication);
        long count = notificationApplicationService.getUnreadNotificationCount(userId);
        return ResponseEntity.ok(ApiResponse.success(new UnreadCountDto(count)));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            Authentication authentication,
            @PathVariable String id
    ) {
        String userId = extractUserId(authentication);
        notificationApplicationService.markAsRead(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu thông báo đã đọc", null));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllAsRead(Authentication authentication) {
        String userId = extractUserId(authentication);
        int updated = notificationApplicationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu tất cả thông báo đã đọc", Map.of("updated", updated)));
    }

    private String extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AuthenticationCredentialsNotFoundException("Yêu cầu đăng nhập để thực hiện thao tác này.");
        }
        String userId = String.valueOf(authentication.getPrincipal());
        if (userId.isBlank() || "anonymousUser".equals(userId)) {
            throw new AuthenticationCredentialsNotFoundException("Yêu cầu đăng nhập để thực hiện thao tác này.");
        }
        return userId;
    }
}

