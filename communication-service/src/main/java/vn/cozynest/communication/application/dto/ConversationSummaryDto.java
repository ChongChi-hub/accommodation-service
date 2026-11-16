package vn.cozynest.communication.application.dto;

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
public class ConversationSummaryDto {
    private String id;
    private String roomId;
    private String roomTitle;
    private String lastMessageContent;
    private LocalDateTime lastMessageTime;
    private long unreadCount;
    private ParticipantDto otherParticipant;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

