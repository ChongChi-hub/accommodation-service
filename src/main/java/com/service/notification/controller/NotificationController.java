package com.service.notification.controller;

import com.service.common.dto.ApiResponse;
import com.service.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") String unreadOnly
    ) {
        String userId = (String) authentication.getPrincipal();
        boolean isUnreadOnly = "1".equals(unreadOnly) || "true".equalsIgnoreCase(unreadOnly);
        Map<String, Object> result = notificationService.listNotifications(userId, isUnreadOnly, page, limit);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            Authentication authentication,
            @PathVariable String id
    ) {
        String userId = (String) authentication.getPrincipal();
        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(ApiResponse.message("Đã đánh dấu đã đọc"));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.message("Đã đánh dấu tất cả đã đọc"));
    }
}

