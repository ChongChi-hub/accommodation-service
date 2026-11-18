package vn.cozynest.communication.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import vn.cozynest.communication.application.dto.*;
import vn.cozynest.communication.application.exception.ChatRateLimitException;
import vn.cozynest.communication.application.exception.ConversationNotFoundException;
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
    @DisplayName("Kiểm tra IDOR: Không phải thành viên tham gia hội thoại thì trả 404 ConversationNotFoundException")
    void idor_check_throwsConversationNotFoundException() {
        when(conversationRepository.findParticipantsByConversationId("conv-secret"))
                .thenReturn(List.of());

        assertThatThrownBy(() -> chatService.getMessages("u-attacker", "conv-secret", 50))
                .isInstanceOf(ConversationNotFoundException.class);

        assertThatThrownBy(() -> chatService.sendMessage("u-attacker", "conv-secret",
                SendMessageRequest.builder().content("Hacked").build()))
                .isInstanceOf(ConversationNotFoundException.class);
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

    @Test
    @DisplayName("Concurrent startOrGetConversation recovers via unique pair key without duplicate")
    void concurrentStartOrGetConversationReturnsWinner() {
        String pairKey = ChatApplicationService.buildPairKey("u-tenant-01", "u-landlord-01", "room-99");
        Conversation winner = Conversation.builder().id("conv-winner").roomId("room-99").participantPairKey(pairKey).build();
        when(conversationRepository.findByParticipantPairKey(pairKey))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winner));
        when(conversationRepository.findByParticipants(anyString(), anyString(), anyString())).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class)))
                .thenThrow(new DataIntegrityViolationException("uq_conversations_pair_key"));
        when(conversationRepository.findParticipantsByConversationId("conv-winner")).thenReturn(List.of());

        ConversationSummaryDto result = chatService.startOrGetConversation("u-tenant-01", "TENANT",
                StartConversationRequest.builder().recipientId("u-landlord-01").roomId("room-99").build());
        assertThat(result.getId()).isEqualTo("conv-winner");
    }

    @Test
    @DisplayName("u-hao-01 is not aliased to u-landlord-02; XSS stored verbatim; length & rate limits enforced")
    void chatValidationRateLimitAndAliasFix() {
        assertThat(ChatApplicationService.getUserAliases("u-landlord-02")).doesNotContain("u-hao-01");

        ConversationParticipant cp = ConversationParticipant.builder()
                .conversationId("conv-1").userId("u-tenant-01").role("TENANT").userName("Tenant").build();
        when(conversationRepository.findParticipantsByConversationId("conv-1")).thenReturn(List.of(cp));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        // XSS payload stored verbatim (escaped on FE display)
        ChatMessageDto xssMsg = chatService.sendMessage("u-tenant-01", "conv-1",
                SendMessageRequest.builder().content("<script>alert(1)</script>").build());
        assertThat(xssMsg.getContent()).isEqualTo("<script>alert(1)</script>");

        // Exceeds 2000 chars
        assertThatThrownBy(() -> chatService.sendMessage("u-tenant-01", "conv-1",
                SendMessageRequest.builder().content("a".repeat(2001)).build()))
                .isInstanceOf(IllegalArgumentException.class);

        // Exceeds 20 msgs / 10s rate limit (1 already sent above -> send 19 more, 21st fails)
        for (int i = 0; i < 19; i++) {
            chatService.sendMessage("u-tenant-01", "conv-1",
                    SendMessageRequest.builder().content("msg " + i).build());
        }
        assertThatThrownBy(() -> chatService.sendMessage("u-tenant-01", "conv-1",
                SendMessageRequest.builder().content("spam").build()))
                .isInstanceOf(ChatRateLimitException.class);
    }
}

