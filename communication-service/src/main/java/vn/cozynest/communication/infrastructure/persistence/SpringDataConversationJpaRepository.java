package vn.cozynest.communication.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataConversationJpaRepository extends JpaRepository<ConversationJpaEntity, String> {

    @Query("""
        SELECT c FROM ConversationJpaEntity c
        WHERE c.id IN (
            SELECT cp.conversationId FROM ConversationParticipantJpaEntity cp
            WHERE cp.userId = :userId
        )
        ORDER BY COALESCE(c.lastMessageTime, c.createdAt) DESC
    """)
    List<ConversationJpaEntity> findAllByUserIdOrderByActivityDesc(@Param("userId") String userId);

    @Query("""
        SELECT c FROM ConversationJpaEntity c
        WHERE c.id IN (
            SELECT cp1.conversationId FROM ConversationParticipantJpaEntity cp1
            WHERE cp1.userId = :userId1
        )
        AND c.id IN (
            SELECT cp2.conversationId FROM ConversationParticipantJpaEntity cp2
            WHERE cp2.userId = :userId2
        )
        AND (:roomId IS NULL OR c.roomId = :roomId)
    """)
    List<ConversationJpaEntity> findByTwoUsersAndOptionalRoom(
            @Param("userId1") String userId1,
            @Param("userId2") String userId2,
            @Param("roomId") String roomId
    );
}

