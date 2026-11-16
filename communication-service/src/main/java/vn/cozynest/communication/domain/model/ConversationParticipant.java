package vn.cozynest.communication.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationParticipant {
    private String id;
    private String conversationId;
    private String userId;
    private String role;
    private String userName;
    private String userAvatar;
    private LocalDateTime lastReadAt;
    private LocalDateTime joinedAt;

    public void markRead(LocalDateTime timestamp) {
        this.lastReadAt = timestamp != null ? timestamp : LocalDateTime.now();
    }
}

