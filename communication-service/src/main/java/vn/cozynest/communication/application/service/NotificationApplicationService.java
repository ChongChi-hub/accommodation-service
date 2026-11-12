package vn.cozynest.communication.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.communication.application.dto.NotificationDto;
import vn.cozynest.communication.application.exception.NotificationNotFoundException;
import vn.cozynest.communication.application.port.NotificationQueryPort;
import vn.cozynest.communication.domain.model.Notification;
import vn.cozynest.communication.domain.repository.ChatSummaryRepository;
import vn.cozynest.communication.domain.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationApplicationService implements NotificationQueryPort {

    private final NotificationRepository notificationRepository;
    private final ChatSummaryRepository chatSummaryRepository;

    public NotificationApplicationService(
            NotificationRepository notificationRepository,
            ChatSummaryRepository chatSummaryRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.chatSummaryRepository = chatSummaryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getNotifications(String userId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        return notificationRepository.findTopByUserIdOrderByCreatedAtDesc(userId, safeLimit).stream()
                .map(NotificationDto::fromDomain)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadNotificationCount(String userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    @Transactional
    public void markAsRead(String userId, String notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .filter(n -> userId.equals(n.getUserId()))
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        notification.markAsRead(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Transactional
    public int markAllAsRead(String userId) {
        return notificationRepository.markAllAsReadByUserId(userId, LocalDateTime.now());
    }

    @Transactional
    public NotificationDto createInternalNotification(String userId, String type, String title, String content, String linkUrl) {
        Notification notification = Notification.builder()
                .id(java.util.UUID.randomUUID().toString())
                .userId(userId)
                .type(type != null && !type.isBlank() ? type : "SYSTEM")
                .title(title)
                .content(content)
                .read(false)
                .linkUrl(linkUrl)
                .createdAt(LocalDateTime.now(java.time.ZoneOffset.UTC))
                .build();
        Notification saved = notificationRepository.save(notification);
        return NotificationDto.fromDomain(saved);
    }

    @Transactional(readOnly = true)
    public long getUnreadChatCount(String userId) {
        return chatSummaryRepository.countUnreadMessagesForUser(userId);
    }
}

