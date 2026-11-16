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
public class ChatMessage {
    private String id;
    private String conversationId;
    private String senderId;
    private String senderName;
    private String content;
    private String type;
    private boolean read;
    private LocalDateTime createdAt;

    public void markRead() {
        this.read = true;
    }
}

