package vn.cozynest.communication.domain.repository;

import vn.cozynest.communication.domain.model.Notification;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository {
    Optional<Notification> findById(String id);
    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);
    Notification save(Notification notification);
}
