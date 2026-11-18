package vn.cozynest.communication.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SpringDataMessageJpaRepository extends JpaRepository<MessageJpaEntity, String> {

    @Query("SELECT m FROM MessageJpaEntity m WHERE m.conversationId = :conversationId ORDER BY m.createdAt ASC")
    List<MessageJpaEntity> findByConversationIdOrderByCreatedAtAsc(@Param("conversationId") String conversationId, Pageable pageable);

    @Query("SELECT m FROM MessageJpaEntity m WHERE m.conversationId = :conversationId AND m.createdAt < :before ORDER BY m.createdAt DESC")
    List<MessageJpaEntity> findOlderMessages(@Param("conversationId") String conversationId, @Param("before") LocalDateTime before, Pageable pageable);

    @Query("""
        SELECT COUNT(m.id) FROM MessageJpaEntity m
        WHERE m.conversationId = :conversationId
          AND m.senderId <> :userId
          AND m.isRead = false
    """)
    long countUnreadMessages(@Param("conversationId") String conversationId, @Param("userId") String userId);

    @Modifying
    @Query("""
        UPDATE MessageJpaEntity m
        SET m.isRead = true
        WHERE m.conversationId = :conversationId
          AND m.senderId <> :userId
          AND m.isRead = false
    """)
    void markMessagesAsRead(@Param("conversationId") String conversationId, @Param("userId") String userId);
}

