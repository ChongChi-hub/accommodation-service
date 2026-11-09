package vn.cozynest.communication.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SpringDataNotificationJpaRepository extends JpaRepository<NotificationJpaEntity, String> {

    List<NotificationJpaEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    List<NotificationJpaEntity> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    long countByUserIdAndIsReadFalse(String userId);

    @Modifying
    @Query("UPDATE NotificationJpaEntity n SET n.isRead = true, n.readAt = :readAt WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") String userId, @Param("readAt") LocalDateTime readAt);
}

