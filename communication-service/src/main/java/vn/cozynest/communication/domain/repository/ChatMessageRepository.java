package vn.cozynest.communication.domain.repository;

import vn.cozynest.communication.domain.model.ChatMessage;

import java.util.List;

public interface ChatMessageRepository {
    ChatMessage save(ChatMessage message);
    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(String conversationId, int limit);
    long countUnreadMessages(String conversationId, String userId);
    void markMessagesAsRead(String conversationId, String currentUserId);
}

