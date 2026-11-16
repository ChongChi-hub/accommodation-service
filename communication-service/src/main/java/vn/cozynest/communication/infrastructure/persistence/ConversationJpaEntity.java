package vn.cozynest.communication.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationJpaEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "room_id", length = 64)
    private String roomId;

    @Column(name = "room_title", length = 255)
    private String roomTitle;

    @Column(name = "last_message_content", columnDefinition = "TEXT")
    private String lastMessageContent;

    @Column(name = "last_message_time")
    private LocalDateTime lastMessageTime;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

