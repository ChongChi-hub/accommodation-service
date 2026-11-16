package vn.cozynest.communication.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SpringDataConversationParticipantJpaRepository extends JpaRepository<ConversationParticipantJpaEntity, String> {

    List<ConversationParticipantJpaEntity> findByConversationId(String conversationId);

    Optional<ConversationParticipantJpaEntity> findByConversationIdAndUserId(String conversationId, String userId);

    @Modifying
    @Query("UPDATE ConversationParticipantJpaEntity cp SET cp.lastReadAt = :readAt WHERE cp.conversationId = :conversationId AND cp.userId = :userId")
    void updateLastReadAt(@Param("conversationId") String conversationId,
                          @Param("userId") String userId,
                          @Param("readAt") LocalDateTime readAt);
}

