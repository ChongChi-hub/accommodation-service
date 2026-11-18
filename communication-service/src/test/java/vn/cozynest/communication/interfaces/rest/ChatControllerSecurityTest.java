package vn.cozynest.communication.interfaces.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.cozynest.communication.application.dto.*;
import vn.cozynest.communication.application.service.ChatApplicationService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ChatControllerSecurityTest {

        private ChatApplicationService chatApplicationService;
        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                chatApplicationService = mock(ChatApplicationService.class);
                ChatController controller = new ChatController(chatApplicationService);

                mockMvc = MockMvcBuilders
                                .standaloneSetup(controller)
                                .setControllerAdvice(new CommunicationExceptionHandler())
                                .build();

                SecurityContextHolder.clearContext();
        }

        private UsernamePasswordAuthenticationToken auth(String userId, String role) {
                return new UsernamePasswordAuthenticationToken(
                                userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        }

        @Test
        @DisplayName("1. GUEST: Truy cập /api/v1/chat/conversations không có xác thực phải trả về 401")
        void getConversations_unauthenticated_returns401() throws Exception {
                mockMvc.perform(get("/api/v1/chat/conversations"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("2. TENANT: Lấy danh sách hội thoại của chính mình thành công")
        void getConversations_authenticated_returns200() throws Exception {
                UsernamePasswordAuthenticationToken tenantAuth = auth("u-tenant-01", "TENANT");

                when(chatApplicationService.getConversations("u-tenant-01")).thenReturn(List.of(
                                ConversationSummaryDto.builder()
                                                .id("conv-0001")
                                                .roomId("room-1")
                                                .roomTitle("Phòng Cầu Giấy")
                                                .lastMessageContent("Chào bạn")
                                                .lastMessageTime(LocalDateTime.now())
                                                .unreadCount(1)
                                                .otherParticipant(ParticipantDto.builder()
                                                                .userId("u-landlord-01")
                                                                .name("Trần Thị Chủ Trọ")
                                                                .role("LANDLORD")
                                                                .build())
                                                .build()));

                mockMvc.perform(get("/api/v1/chat/conversations").principal(tenantAuth))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].id").value("conv-0001"))
                                .andExpect(jsonPath("$.data[0].otherParticipant.name").value("Trần Thị Chủ Trọ"));
        }

        @Test
        @DisplayName("3. IDOR: Kẻ lạ cố đọc tin nhắn trong cuộc trò chuyện của người khác bị chặn 403 Forbidden")
        void getMessages_idorAttack_returns403() throws Exception {
                UsernamePasswordAuthenticationToken attackerAuth = auth("u-attacker", "TENANT");

                when(chatApplicationService.getMessages(eq("u-attacker"), eq("conv-private"), anyInt()))
                                .thenThrow(new vn.cozynest.communication.application.exception.ConversationNotFoundException("conv-private"));

                mockMvc.perform(get("/api/v1/chat/conversations/conv-private/messages").principal(attackerAuth))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.title").value("Conversation Not Found"));
        }

        @Test
        @DisplayName("4. Gửi tin nhắn thành công trong cuộc hội thoại hợp lệ")
        void sendMessage_validParticipant_returns201() throws Exception {
                UsernamePasswordAuthenticationToken tenantAuth = auth("u-tenant-01", "TENANT");

                when(chatApplicationService.sendMessage(eq("u-tenant-01"), eq("conv-0001"),
                                any(SendMessageRequest.class)))
                                .thenReturn(ChatMessageDto.builder()
                                                .id("msg-01")
                                                .conversationId("conv-0001")
                                                .senderId("u-tenant-01")
                                                .senderName("Nguyễn Văn Thuê")
                                                .content("Phòng này còn trống không ạ?")
                                                .isMine(true)
                                                .createdAt(LocalDateTime.now())
                                                .build());

                mockMvc.perform(post("/api/v1/chat/conversations/conv-0001/messages")
                                .principal(tenantAuth)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"content\":\"Phòng này còn trống không ạ?\"}"))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data.content").value("Phòng này còn trống không ạ?"))
                                .andExpect(jsonPath("$.data.isMine").value(true));
        }

        @Test
        @DisplayName("5. Đánh dấu đã đọc thành công qua PATCH")
        void markAsRead_validParticipant_returns200() throws Exception {
                UsernamePasswordAuthenticationToken tenantAuth = auth("u-tenant-01", "TENANT");

                doNothing().when(chatApplicationService).markAsRead("u-tenant-01", "conv-0001");

                mockMvc.perform(patch("/api/v1/chat/conversations/conv-0001/read").principal(tenantAuth))
                                .andExpect(status().isOk());
        }
}