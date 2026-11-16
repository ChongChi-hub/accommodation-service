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
public class Conversation {
    private String id;
    private String roomId;
    private String roomTitle;
    private String lastMessageContent;
    private LocalDateTime lastMessageTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void recordNewMessage(String content, LocalDateTime timestamp) {
        this.lastMessageContent = content;
        this.lastMessageTime = timestamp != null ? timestamp : LocalDateTime.now();
        this.updatedAt = this.lastMessageTime;
    }
}

