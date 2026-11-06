package com.service.notification.service;

import com.service.common.exception.AppException;
import com.service.notification.repository.ConversationRepository;
import com.service.notification.repository.MessageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final JdbcTemplate jdbcTemplate;

    public ChatService(ConversationRepository conversationRepository, MessageRepository messageRepository, JdbcTemplate jdbcTemplate) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public String getOrCreateConversation(String tenantId, String landlordId, String roomId) {
        String querySql = roomId != null
                ? "SELECT id FROM conversations WHERE tenant_id = ? AND landlord_id = ? AND room_id = ?"
                : "SELECT id FROM conversations WHERE tenant_id = ? AND landlord_id = ? AND room_id IS NULL";

        List<Map<String, Object>> existing = roomId != null
                ? jdbcTemplate.queryForList(querySql, tenantId, landlordId, roomId)
                : jdbcTemplate.queryForList(querySql, tenantId, landlordId);

        if (!existing.isEmpty()) {
            return (String) existing.get(0).get("id");
        }

        String convId = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO conversations (id, room_id, tenant_id, landlord_id) VALUES (?, ?, ?, ?)",
                convId, roomId, tenantId, landlordId
        );
        return convId;
    }

    public List<Map<String, Object>> listConversations(String userId) {
        String sql = """
            SELECT c.id, c.room_id, c.created_at,
                   t.id as tenant_id, t.full_name as tenant_name, t.avatar_url as tenant_avatar,
                   l.id as landlord_id, l.full_name as landlord_name, l.avatar_url as landlord_avatar,
                   r.title as room_title,
                   (SELECT content FROM messages m WHERE m.conversation_id = c.id ORDER BY m.created_at DESC LIMIT 1) as last_message,
                   (SELECT created_at FROM messages m WHERE m.conversation_id = c.id ORDER BY m.created_at DESC LIMIT 1) as last_message_at,
                   (SELECT COUNT(*) FROM messages m WHERE m.conversation_id = c.id AND m.is_read = 0 AND m.sender_id != ?) as unread_count
            FROM conversations c
            JOIN users t ON c.tenant_id = t.id
            JOIN users l ON c.landlord_id = l.id
            LEFT JOIN rooms r ON c.room_id = r.id
            WHERE c.tenant_id = ? OR c.landlord_id = ?
            ORDER BY last_message_at DESC
        """;
        return jdbcTemplate.queryForList(sql, userId, userId, userId);
    }

    @Transactional
    public List<Map<String, Object>> getMessages(String conversationId, String userId, int page, int limit) {
        int offset = (page - 1) * limit;
        String sql = """
            SELECT m.id, m.sender_id, m.content, m.is_read, m.created_at,
                   u.full_name as sender_name, u.avatar_url as sender_avatar
            FROM messages m
            JOIN users u ON m.sender_id = u.id
            WHERE m.conversation_id = ?
            ORDER BY m.created_at DESC LIMIT ? OFFSET ?
        """;
        List<Map<String, Object>> messages = jdbcTemplate.queryForList(sql, conversationId, limit, offset);

        // Mark as read
        jdbcTemplate.update(
                "UPDATE messages SET is_read = 1 WHERE conversation_id = ? AND sender_id != ?",
                conversationId, userId
        );

        // Reverse to chronological order (oldest first)
        Collections.reverse(messages);
        return messages;
    }

    @Transactional
    public Map<String, Object> sendMessage(String conversationId, String senderId, String content) {
        List<Map<String, Object>> convs = jdbcTemplate.queryForList(
                "SELECT tenant_id, landlord_id FROM conversations WHERE id = ?",
                conversationId
        );
        if (convs.isEmpty()) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy cuộc hội thoại");
        }

        Map<String, Object> conv = convs.get(0);
        String tenantId = (String) conv.get("tenant_id");
        String landlordId = (String) conv.get("landlord_id");

        if (!senderId.equals(tenantId) && !senderId.equals(landlordId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "Bạn không thuộc cuộc hội thoại này");
        }

        String messageId = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO messages (id, conversation_id, sender_id, content) VALUES (?, ?, ?, ?)",
                messageId, conversationId, senderId, content
        );

        // Notify recipient
        String recipientId = senderId.equals(tenantId) ? landlordId : tenantId;
        String preview = content.length() > 100 ? content.substring(0, 100) : content;
        jdbcTemplate.update(
                "INSERT INTO notifications (id, user_id, type, title, body, ref_id) VALUES (?, ?, 'new_message', 'Tin nhắn mới', ?, ?)",
                UUID.randomUUID().toString(),
                recipientId,
                preview,
                conversationId
        );

        return Map.of(
                "id", messageId,
                "senderId", senderId,
                "content", content,
                "createdAt", LocalDateTime.now().toString()
        );
    }
}

