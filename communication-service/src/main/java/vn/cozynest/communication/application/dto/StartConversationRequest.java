package vn.cozynest.communication.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartConversationRequest {
    @NotBlank(message = "Người nhận không được để trống")
    private String recipientId;

    private String recipientName;
    private String recipientRole;
    private String senderName;
    private String roomId;
    private String roomTitle;
    private String initialMessage;
}

