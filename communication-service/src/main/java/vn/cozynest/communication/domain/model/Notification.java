package vn.cozynest.communication.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    private String id;
    private String userId;
    private String type;
    private String title;
    private String content;
    private boolean read;
    private String linkUrl;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;

    public void markAsRead(LocalDateTime timestamp) {
        if (!this.read) {
            this.read = true;
            this.readAt = timestamp != null ? timestamp : LocalDateTime.now();
        }
    }
}
