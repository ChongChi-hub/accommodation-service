package vn.cozynest.communication.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class ChatMessageDto {
    private String id;
    private String conversationId;
    private String senderId;
    private String senderName;
    private String content;
    private String type;

    @JsonProperty("isRead")
    private boolean isRead;

    @JsonProperty("isMine")
    private boolean isMine;

    private LocalDateTime createdAt;
}

