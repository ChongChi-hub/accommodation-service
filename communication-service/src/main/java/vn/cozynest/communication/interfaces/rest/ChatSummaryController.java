package vn.cozynest.communication.interfaces.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.communication.application.dto.UnreadCountDto;
import vn.cozynest.communication.application.service.NotificationApplicationService;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatSummaryController {

    private final NotificationApplicationService notificationApplicationService;

    public ChatSummaryController(NotificationApplicationService notificationApplicationService) {
        this.notificationApplicationService = notificationApplicationService;
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountDto>> getUnreadCount(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AuthenticationCredentialsNotFoundException("Yêu cầu đăng nhập để xem số tin nhắn chưa đọc.");
        }
        String userId = String.valueOf(authentication.getPrincipal());
        if (userId.isBlank() || "anonymousUser".equals(userId)) {
            throw new AuthenticationCredentialsNotFoundException("Yêu cầu đăng nhập để xem số tin nhắn chưa đọc.");
        }
        long count = notificationApplicationService.getUnreadChatCount(userId);
        return ResponseEntity.ok(ApiResponse.success(new UnreadCountDto(count)));
    }
}

