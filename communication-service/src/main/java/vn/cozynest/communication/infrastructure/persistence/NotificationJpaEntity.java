package vn.cozynest.communication.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationJpaEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    @Column(name = "link_url", length = 500)
    private String linkUrl;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

