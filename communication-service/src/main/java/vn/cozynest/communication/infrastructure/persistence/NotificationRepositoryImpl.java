package vn.cozynest.communication.infrastructure.persistence;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import vn.cozynest.communication.domain.model.Notification;
import vn.cozynest.communication.domain.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class NotificationRepositoryImpl implements NotificationRepository {

    private final SpringDataNotificationJpaRepository jpaRepository;

    public NotificationRepositoryImpl(SpringDataNotificationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Notification> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Notification> findByUserIdOrderByCreatedAtDesc(String userId) {
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Notification> findTopByUserIdOrderByCreatedAtDesc(String userId, int limit) {
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit)).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countUnreadByUserId(String userId) {
        return jpaRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    public int markAllAsReadByUserId(String userId, LocalDateTime readAt) {
        return jpaRepository.markAllAsReadByUserId(userId, readAt);
    }

    @Override
    public Notification save(Notification n) {
        NotificationJpaEntity entity = NotificationJpaEntity.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .type(n.getType())
                .title(n.getTitle())
                .content(n.getContent())
                .isRead(n.isRead())
                .linkUrl(n.getLinkUrl())
                .readAt(n.getReadAt())
                .createdAt(n.getCreatedAt() != null ? n.getCreatedAt() : LocalDateTime.now())
                .build();
        return toDomain(jpaRepository.save(entity));
    }

    private Notification toDomain(NotificationJpaEntity e) {
        return Notification.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .type(e.getType())
                .title(e.getTitle())
                .content(e.getContent())
                .read(e.isRead())
                .linkUrl(e.getLinkUrl())
                .readAt(e.getReadAt())
                .createdAt(e.getCreatedAt())
                .build();
    }
}

