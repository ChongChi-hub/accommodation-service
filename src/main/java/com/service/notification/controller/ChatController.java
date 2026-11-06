package com.service.notification.controller;

import com.service.common.dto.ApiResponse;
import com.service.notification.dto.CreateConversationRequest;
import com.service.notification.dto.SendMessageRequest;
import com.service.notification.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/conversations")
    public ResponseEntity<ApiResponse<Map<String, String>>> getOrCreateConversation(
            Authentication authentication,
            @Valid @RequestBody CreateConversationRequest request
    ) {
        String tenantId = (String) authentication.getPrincipal();
        String convId = chatService.getOrCreateConversation(tenantId, request.getLandlordId(), request.getRoomId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(Map.of("id", convId)));
    }

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listConversations(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        List<Map<String, Object>> convs = chatService.listConversations(userId);
        return ResponseEntity.ok(ApiResponse.success(convs));
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMessages(
            Authentication authentication,
            @PathVariable String id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int limit
    ) {
        String userId = (String) authentication.getPrincipal();
        List<Map<String, Object>> messages = chatService.getMessages(id, userId, page, limit);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendMessage(
            Authentication authentication,
            @PathVariable String id,
            @Valid @RequestBody SendMessageRequest request
    ) {
        String senderId = (String) authentication.getPrincipal();
        Map<String, Object> data = chatService.sendMessage(id, senderId, request.getContent());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(data));
    }
}

