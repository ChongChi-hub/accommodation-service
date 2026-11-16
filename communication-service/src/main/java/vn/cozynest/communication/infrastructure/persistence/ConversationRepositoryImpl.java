package vn.cozynest.communication.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import vn.cozynest.communication.domain.model.Conversation;
import vn.cozynest.communication.domain.model.ConversationParticipant;
import vn.cozynest.communication.domain.repository.ConversationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ConversationRepositoryImpl implements ConversationRepository {

    private final SpringDataConversationJpaRepository conversationJpaRepo;
    private final SpringDataConversationParticipantJpaRepository participantJpaRepo;

    public ConversationRepositoryImpl(
            SpringDataConversationJpaRepository conversationJpaRepo,
            SpringDataConversationParticipantJpaRepository participantJpaRepo
    ) {
        this.conversationJpaRepo = conversationJpaRepo;
        this.participantJpaRepo = participantJpaRepo;
    }

    @Override
    public Optional<Conversation> findById(String id) {
        return conversationJpaRepo.findById(id).map(this::toDomain);
    }

    @Override
    public List<Conversation> findByUserId(String userId) {
        return conversationJpaRepo.findAllByUserIdOrderByActivityDesc(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Conversation> findByParticipants(String userId1, String userId2, String roomId) {
        List<ConversationJpaEntity> list = conversationJpaRepo.findByTwoUsersAndOptionalRoom(userId1, userId2, roomId);
        if (list.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(toDomain(list.get(0)));
    }

    @Override
    public Conversation save(Conversation domain) {
        ConversationJpaEntity entity = toJpa(domain);
        ConversationJpaEntity saved = conversationJpaRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<ConversationParticipant> findParticipantsByConversationId(String conversationId) {
        return participantJpaRepo.findByConversationId(conversationId).stream()
                .map(this::toParticipantDomain)
                .toList();
    }

    @Override
    public Optional<ConversationParticipant> findParticipant(String conversationId, String userId) {
        return participantJpaRepo.findByConversationIdAndUserId(conversationId, userId)
                .map(this::toParticipantDomain);
    }

    @Override
    public ConversationParticipant saveParticipant(ConversationParticipant domain) {
        ConversationParticipantJpaEntity entity = toParticipantJpa(domain);
        ConversationParticipantJpaEntity saved = participantJpaRepo.save(entity);
        return toParticipantDomain(saved);
    }

    @Override
    public void updateLastRead(String conversationId, String userId, LocalDateTime readAt) {
        participantJpaRepo.updateLastReadAt(conversationId, userId, readAt);
    }

    @Override
    public void updateLastMessage(String conversationId, String content, LocalDateTime time) {
        conversationJpaRepo.findById(conversationId).ifPresent(c -> {
            c.setLastMessageContent(content);
            c.setLastMessageTime(time);
            c.setUpdatedAt(time != null ? time : LocalDateTime.now());
            conversationJpaRepo.save(c);
        });
    }

    private Conversation toDomain(ConversationJpaEntity entity) {
        return Conversation.builder()
                .id(entity.getId())
                .roomId(entity.getRoomId())
                .roomTitle(entity.getRoomTitle())
                .lastMessageContent(entity.getLastMessageContent())
                .lastMessageTime(entity.getLastMessageTime())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private ConversationJpaEntity toJpa(Conversation domain) {
        return ConversationJpaEntity.builder()
                .id(domain.getId())
                .roomId(domain.getRoomId())
                .roomTitle(domain.getRoomTitle())
                .lastMessageContent(domain.getLastMessageContent())
                .lastMessageTime(domain.getLastMessageTime())
                .createdAt(domain.getCreatedAt() != null ? domain.getCreatedAt() : LocalDateTime.now())
                .updatedAt(domain.getUpdatedAt() != null ? domain.getUpdatedAt() : LocalDateTime.now())
                .build();
    }

    private ConversationParticipant toParticipantDomain(ConversationParticipantJpaEntity entity) {
        return ConversationParticipant.builder()
                .id(entity.getId())
                .conversationId(entity.getConversationId())
                .userId(entity.getUserId())
                .role(entity.getRole())
                .userName(entity.getUserName())
                .userAvatar(entity.getUserAvatar())
                .lastReadAt(entity.getLastReadAt())
                .joinedAt(entity.getJoinedAt())
                .build();
    }

    private ConversationParticipantJpaEntity toParticipantJpa(ConversationParticipant domain) {
        return ConversationParticipantJpaEntity.builder()
                .id(domain.getId())
                .conversationId(domain.getConversationId())
                .userId(domain.getUserId())
                .role(domain.getRole())
                .userName(domain.getUserName())
                .userAvatar(domain.getUserAvatar())
                .lastReadAt(domain.getLastReadAt())
                .joinedAt(domain.getJoinedAt() != null ? domain.getJoinedAt() : LocalDateTime.now())
                .build();
    }
}

