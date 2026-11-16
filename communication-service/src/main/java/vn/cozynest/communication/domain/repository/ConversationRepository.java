package vn.cozynest.communication.domain.repository;

import vn.cozynest.communication.domain.model.Conversation;
import vn.cozynest.communication.domain.model.ConversationParticipant;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ConversationRepository {
    Optional<Conversation> findById(String id);
    List<Conversation> findByUserId(String userId);
    Optional<Conversation> findByParticipants(String userId1, String userId2, String roomId);
    Conversation save(Conversation conversation);
    List<ConversationParticipant> findParticipantsByConversationId(String conversationId);
    Optional<ConversationParticipant> findParticipant(String conversationId, String userId);
    ConversationParticipant saveParticipant(ConversationParticipant participant);
    void updateLastRead(String conversationId, String userId, LocalDateTime readAt);
    void updateLastMessage(String conversationId, String content, LocalDateTime time);
}

