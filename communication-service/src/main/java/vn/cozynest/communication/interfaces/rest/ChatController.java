package vn.cozynest.communication.interfaces.rest;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.communication.application.dto.*;
import vn.cozynest.communication.application.service.ChatApplicationService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatApplicationService chatApplicationService;

    public ChatController(ChatApplicationService chatApplicationService) {
        this.chatApplicationService = chatApplicationService;
    }

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<ConversationSummaryDto>>> getConversations(Authentication authentication) {
        String userId = extractUserId(authentication);
        List<ConversationSummaryDto> list = chatApplicationService.getConversations(userId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping("/conversations")
    public ResponseEntity<ApiResponse<ConversationSummaryDto>> startOrGetConversation(
            Authentication authentication,
            @Valid @RequestBody StartConversationRequest request
    ) {
        String userId = extractUserId(authentication);
        String role = extractPrimaryRole(authentication);
        ConversationSummaryDto summary = chatApplicationService.startOrGetConversation(userId, role, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Bắt đầu cuộc trò chuyện thành công", summary));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageDto>>> getMessages(
            Authentication authentication,
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        String userId = extractUserId(authentication);
        List<ChatMessageDto> messages = chatApplicationService.getMessages(userId, conversationId, limit);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<ChatMessageDto>> sendMessage(
            Authentication authentication,
            @PathVariable String conversationId,
            @Valid @RequestBody SendMessageRequest request
    ) {
        String userId = extractUserId(authentication);
        ChatMessageDto sent = chatApplicationService.sendMessage(userId, conversationId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Gửi tin nhắn thành công", sent));
    }

    @RequestMapping(value = "/conversations/{conversationId}/read", method = {RequestMethod.PATCH, RequestMethod.PUT})
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            Authentication authentication,
            @PathVariable String conversationId
    ) {
        String userId = extractUserId(authentication);
        chatApplicationService.markAsRead(userId, conversationId);
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu đã đọc", null));
    }

    private String extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AuthenticationCredentialsNotFoundException("Yêu cầu đăng nhập.");
        }
        String userId = String.valueOf(authentication.getPrincipal());
        if (userId.isBlank() || "anonymousUser".equals(userId)) {
            throw new AuthenticationCredentialsNotFoundException("Yêu cầu đăng nhập.");
        }
        return userId;
    }

    private String extractPrimaryRole(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return "TENANT";
        }
        for (GrantedAuthority ga : authentication.getAuthorities()) {
            String auth = ga.getAuthority();
            if ("ROLE_LANDLORD".equalsIgnoreCase(auth) || "LANDLORD".equalsIgnoreCase(auth)) {
                return "LANDLORD";
            }
            if ("ROLE_ADMIN".equalsIgnoreCase(auth) || "ADMIN".equalsIgnoreCase(auth)) {
                return "ADMIN";
            }
        }
        return "TENANT";
    }
}

