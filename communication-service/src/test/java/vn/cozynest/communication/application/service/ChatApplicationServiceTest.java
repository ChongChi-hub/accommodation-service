package vn.cozynest.communication.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import vn.cozynest.communication.application.dto.*;
import vn.cozynest.communication.domain.model.ChatMessage;
import vn.cozynest.communication.domain.model.Conversation;
import vn.cozynest.communication.domain.model.ConversationParticipant;
import vn.cozynest.communication.domain.repository.ChatMessageRepository;
import vn.cozynest.communication.domain.repository.ConversationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatApplicationServiceTest {

    private ConversationRepository conversationRepository;
    private ChatMessageRepository chatMessageRepository;
    private ChatApplicationService chatService;

    @BeforeEach
    void setUp() {
        conversationRepository = mock(ConversationRepository.class);
        chatMessageRepository = mock(ChatMessageRepository.class);
        chatService = new ChatApplicationService(conversationRepository, chatMessageRepository);
    }

    @Test
    @DisplayName("Khởi tạo cuộc hội thoại mới giữa khách thuê và chủ nhà với tin nhắn mở đầu")
    void startOrGetConversation_newConversation_createsParticipantsAndMessage() {
        StartConversationRequest req = StartConversationRequest.builder()
                .recipientId("u-landlord-01")
                .recipientName("Trần Thị Chủ Trọ")
                .recipientRole("LANDLORD")
                .senderName("Nguyễn Văn Thuê")
                .roomId("room-101")
                .roomTitle("Phòng Studio Cầu Giấy")
                .initialMessage("Chào chủ trọ, tôi muốn hỏi thăm phòng")
                .build();

        when(conversationRepository.findByParticipants("u-tenant-01", "u-landlord-01", "room-101"))
                .thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        ConversationParticipant cpSender = ConversationParticipant.builder()
                .conversationId("conv-1")
                .userId("u-tenant-01")
                .role("TENANT")
                .userName("Nguyễn Văn Thuê")
                .build();
        ConversationParticipant cpRec = ConversationParticipant.builder()
                .conversationId("conv-1")
                .userId("u-landlord-01")
                .role("LANDLORD")
                .userName("Trần Thị Chủ Trọ")
                .build();

        when(conversationRepository.findParticipantsByConversationId(anyString()))
                .thenReturn(List.of(cpSender, cpRec));
        when(conversationRepository.findParticipant(anyString(), eq("u-tenant-01")))
                .thenReturn(Optional.of(cpSender));

        ConversationSummaryDto summary = chatService.startOrGetConversation("u-tenant-01", "TENANT", req);

        assertThat(summary).isNotNull();
        assertThat(summary.getRoomTitle()).isEqualTo("Phòng Studio Cầu Giấy");
        assertThat(summary.getOtherParticipant().getUserId()).isEqualTo("u-landlord-01");
        assertThat(summary.getOtherParticipant().getName()).isEqualTo("Trần Thị Chủ Trọ");
        verify(chatMessageRepository, atLeastOnce()).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("Kiểm tra IDOR: Không phải thành viên tham gia hội thoại thì bị ném AccessDeniedException")
    void idor_check_throwsAccessDeniedException() {
        when(conversationRepository.findParticipant("conv-secret", "u-attacker"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.getMessages("u-attacker", "conv-secret", 50))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Bạn không có quyền truy cập");

        assertThatThrownBy(() -> chatService.sendMessage("u-attacker", "conv-secret",
                SendMessageRequest.builder().content("Hacked").build()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Không được tự nhắn tin cho chính mình")
    void self_chat_throwsIllegalArgumentException() {
        StartConversationRequest req = StartConversationRequest.builder()
                .recipientId("u-tenant-01")
                .build();

        assertThatThrownBy(() -> chatService.startOrGetConversation("u-tenant-01", "TENANT", req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Không thể tự nhắn tin cho chính mình");
    }
}

