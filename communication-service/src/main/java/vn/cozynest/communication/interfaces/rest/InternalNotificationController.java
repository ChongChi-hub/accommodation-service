package vn.cozynest.communication.interfaces.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.communication.application.dto.NotificationDto;
import vn.cozynest.communication.application.service.NotificationApplicationService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/internal/notifications")
public class InternalNotificationController {

    private static final String DEFAULT_INTERNAL_KEY = "CozyNestInternalServiceKeySecret2026";

    private final NotificationApplicationService notificationApplicationService;
    private final String expectedServiceKey;

    public InternalNotificationController(
            NotificationApplicationService notificationApplicationService,
            @Value("${app.internal.service-key:" + DEFAULT_INTERNAL_KEY + "}") String expectedServiceKey,
            @Autowired(required = false) Environment env) {
        this.notificationApplicationService = notificationApplicationService;
        this.expectedServiceKey = expectedServiceKey;

        if (env != null && env.matchesProfiles("prod", "production")) {
            String envKey = System.getenv("INTERNAL_SERVICE_KEY");
            if (envKey == null || envKey.isBlank() || DEFAULT_INTERNAL_KEY.equals(expectedServiceKey)) {
                throw new IllegalStateException("Production environment must explicitly configure INTERNAL_SERVICE_KEY without default dev key");
            }
        }
    }

    public record CreateNotificationRequest(
            String userId,
            String type,
            String title,
            String content,
            String linkUrl
    ) {}

    @PostMapping
    public ResponseEntity<ApiResponse<NotificationDto>> createNotification(
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String serviceKey,
            @RequestBody CreateNotificationRequest request) {
        if (serviceKey == null || !isKeyValid(serviceKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Truy cập nội bộ không hợp lệ", "ERR_UNAUTHORIZED"));
        }
        if (request == null || request.userId() == null || request.userId().isBlank() || request.title() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Thiếu thông tin người nhận hoặc tiêu đề thông báo", "ERR_INVALID_REQUEST"));
        }
        NotificationDto dto = notificationApplicationService.createInternalNotification(
                request.userId(),
                request.type(),
                request.title(),
                request.content() != null ? request.content() : request.title(),
                request.linkUrl()
        );
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    private boolean isKeyValid(String serviceKey) {
        if (expectedServiceKey == null) return false;
        return MessageDigest.isEqual(
                serviceKey.getBytes(StandardCharsets.UTF_8),
                expectedServiceKey.getBytes(StandardCharsets.UTF_8)
        );
    }
}

