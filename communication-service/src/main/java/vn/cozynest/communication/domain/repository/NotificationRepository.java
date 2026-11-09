package vn.cozynest.communication.domain.repository;

import vn.cozynest.communication.domain.model.Notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository {
    Optional<Notification> findById(String id);
    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);
    List<Notification> findTopByUserIdOrderByCreatedAtDesc(String userId, int limit);
    long countUnreadByUserId(String userId);
    int markAllAsReadByUserId(String userId, LocalDateTime readAt);
    Notification save(Notification notification);
}
