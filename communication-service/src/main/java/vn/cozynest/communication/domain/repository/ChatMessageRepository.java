package vn.cozynest.communication.domain.repository;

import vn.cozynest.communication.domain.model.ChatMessage;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageRepository {
    ChatMessage save(ChatMessage message);
    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(String conversationId, int limit);
    default List<ChatMessage> findByConversationIdBefore(String conversationId, LocalDateTime before, int limit) {
        return findByConversationIdOrderByCreatedAtAsc(conversationId, limit);
    }
    long countUnreadMessages(String conversationId, String userId);
    void markMessagesAsRead(String conversationId, String currentUserId);
}

