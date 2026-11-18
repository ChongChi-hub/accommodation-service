package vn.cozynest.communication.infrastructure.persistence;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import vn.cozynest.communication.domain.model.ChatMessage;
import vn.cozynest.communication.domain.repository.ChatMessageRepository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class ChatMessageRepositoryImpl implements ChatMessageRepository {

    private final SpringDataMessageJpaRepository messageJpaRepo;

    public ChatMessageRepositoryImpl(SpringDataMessageJpaRepository messageJpaRepo) {
        this.messageJpaRepo = messageJpaRepo;
    }

    @Override
    public ChatMessage save(ChatMessage domain) {
        MessageJpaEntity entity = toJpa(domain);
        MessageJpaEntity saved = messageJpaRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(String conversationId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return messageJpaRepo.findByConversationIdOrderByCreatedAtAsc(conversationId, PageRequest.of(0, safeLimit)).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<ChatMessage> findByConversationIdBefore(String conversationId, LocalDateTime before, int limit) {
        if (before == null) {
            return findByConversationIdOrderByCreatedAtAsc(conversationId, limit);
        }
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        List<ChatMessage> desc = messageJpaRepo.findOlderMessages(conversationId, before, PageRequest.of(0, safeLimit))
                .stream()
                .map(this::toDomain)
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        java.util.Collections.reverse(desc);
        return desc;
    }

    @Override
    public long countUnreadMessages(String conversationId, String userId) {
        return messageJpaRepo.countUnreadMessages(conversationId, userId);
    }

    @Override
    public void markMessagesAsRead(String conversationId, String currentUserId) {
        messageJpaRepo.markMessagesAsRead(conversationId, currentUserId);
    }

    private ChatMessage toDomain(MessageJpaEntity entity) {
        return ChatMessage.builder()
                .id(entity.getId())
                .conversationId(entity.getConversationId())
                .senderId(entity.getSenderId())
                .content(entity.getContent())
                .type(entity.getType())
                .read(entity.isRead())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private MessageJpaEntity toJpa(ChatMessage domain) {
        return MessageJpaEntity.builder()
                .id(domain.getId())
                .conversationId(domain.getConversationId())
                .senderId(domain.getSenderId())
                .content(domain.getContent())
                .type(domain.getType() != null ? domain.getType() : "TEXT")
                .isRead(domain.isRead())
                .createdAt(domain.getCreatedAt() != null ? domain.getCreatedAt() : LocalDateTime.now())
                .build();
    }
}

