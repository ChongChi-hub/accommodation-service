package vn.cozynest.communication.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import vn.cozynest.communication.domain.repository.ChatSummaryRepository;

@Repository
public class ChatSummaryRepositoryImpl implements ChatSummaryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public long countUnreadMessagesForUser(String userId) {
        if (userId == null || userId.isBlank()) {
            return 0L;
        }
        Number count = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(m.id)
                FROM messages m
                INNER JOIN conversation_participants cp
                    ON cp.conversation_id = m.conversation_id AND cp.user_id = :userId
                WHERE m.sender_id <> :userId
                  AND (m.is_read = 0 AND (cp.last_read_at IS NULL OR m.created_at > cp.last_read_at))
                """)
                .setParameter("userId", userId)
                .getSingleResult();
        return count != null ? count.longValue() : 0L;
    }
}

