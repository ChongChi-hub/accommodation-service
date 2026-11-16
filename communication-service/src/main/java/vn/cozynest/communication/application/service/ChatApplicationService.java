package vn.cozynest.communication.application.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.communication.application.dto.*;
import vn.cozynest.communication.domain.model.ChatMessage;
import vn.cozynest.communication.domain.model.Conversation;
import vn.cozynest.communication.domain.model.ConversationParticipant;
import vn.cozynest.communication.domain.repository.ChatMessageRepository;
import vn.cozynest.communication.domain.repository.ConversationRepository;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ChatApplicationService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;

    public ChatApplicationService(
            ConversationRepository conversationRepository,
            ChatMessageRepository chatMessageRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Transactional(readOnly = true)
    public List<ConversationSummaryDto> getConversations(String currentUserId) {
        List<Conversation> conversations = conversationRepository.findByUserId(currentUserId);
        List<ConversationSummaryDto> summaries = new ArrayList<>();

        for (Conversation c : conversations) {
            List<ConversationParticipant> participants = conversationRepository.findParticipantsByConversationId(c.getId());
            ConversationParticipant other = null;
            for (ConversationParticipant p : participants) {
                if (!currentUserId.equals(p.getUserId())) {
                    other = p;
                    break;
                }
            }

            ParticipantDto otherDto = null;
            if (other != null) {
                String displayName = other.getUserName();
                if (displayName == null || displayName.isBlank()) {
                    displayName = "Người dùng " + other.getUserId();
                }
                otherDto = ParticipantDto.builder()
                        .userId(other.getUserId())
                        .name(displayName)
                        .role(other.getRole())
                        .avatarUrl(other.getUserAvatar())
                        .build();
            }

            long unreadCount = chatMessageRepository.countUnreadMessages(c.getId(), currentUserId);

            summaries.add(ConversationSummaryDto.builder()
                    .id(c.getId())
                    .roomId(c.getRoomId())
                    .roomTitle(c.getRoomTitle())
                    .lastMessageContent(c.getLastMessageContent())
                    .lastMessageTime(c.getLastMessageTime())
                    .unreadCount(unreadCount)
                    .otherParticipant(otherDto)
                    .createdAt(c.getCreatedAt())
                    .updatedAt(c.getUpdatedAt())
                    .build());
        }

        return summaries;
    }

    @Transactional
    public ConversationSummaryDto startOrGetConversation(String currentUserId, String currentRole, StartConversationRequest request) {
        String recipientId = request.getRecipientId();
        if (recipientId != null && recipientId.matches("\\d+")) {
            int num = Integer.parseInt(recipientId);
            recipientId = num < 10 ? ("u-landlord-0" + num) : ("u-landlord-" + num);
        }

        if (currentUserId.equals(recipientId)) {
            throw new IllegalArgumentException("Không thể tự nhắn tin cho chính mình.");
        }

        Optional<Conversation> existing = conversationRepository.findByParticipants(currentUserId, recipientId, request.getRoomId());
        Conversation conversation;

        if (existing.isPresent()) {
            conversation = existing.get();
            // Update room title if missing
            if ((conversation.getRoomTitle() == null || conversation.getRoomTitle().isBlank()) && request.getRoomTitle() != null) {
                conversation.setRoomTitle(request.getRoomTitle());
                conversationRepository.save(conversation);
            }
        } else {
            String convId = "conv-" + UUID.randomUUID().toString().substring(0, 8);
            LocalDateTime now = LocalDateTime.now();

            conversation = Conversation.builder()
                    .id(convId)
                    .roomId(request.getRoomId())
                    .roomTitle(request.getRoomTitle())
                    .lastMessageContent(request.getInitialMessage())
                    .lastMessageTime(request.getInitialMessage() != null ? now : null)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            conversationRepository.save(conversation);

            // Create current user participant
            String senderRole = currentRole != null && !currentRole.isBlank() ? currentRole : "TENANT";
            String senderName = request.getSenderName();
            if (senderName == null || senderName.isBlank()) {
                senderName = "Người dùng " + currentUserId;
            }
            ConversationParticipant senderParticipant = ConversationParticipant.builder()
                    .id("cp-" + UUID.randomUUID().toString().substring(0, 8))
                    .conversationId(convId)
                    .userId(currentUserId)
                    .role(senderRole)
                    .userName(senderName)
                    .lastReadAt(now)
                    .joinedAt(now)
                    .build();
            conversationRepository.saveParticipant(senderParticipant);

            // Create recipient participant
            String recRole = request.getRecipientRole() != null && !request.getRecipientRole().isBlank()
                    ? request.getRecipientRole()
                    : ("TENANT".equals(senderRole) ? "LANDLORD" : "TENANT");
            String recName = request.getRecipientName();
            if (recName == null || recName.isBlank()) {
                recName = "Người dùng " + recipientId;
            }
            ConversationParticipant recParticipant = ConversationParticipant.builder()
                    .id("cp-" + UUID.randomUUID().toString().substring(0, 8))
                    .conversationId(convId)
                    .userId(recipientId)
                    .role(recRole)
                    .userName(recName)
                    .joinedAt(now)
                    .build();
            conversationRepository.saveParticipant(recParticipant);
        }

        // If there is an initial message, send it
        if (request.getInitialMessage() != null && !request.getInitialMessage().trim().isEmpty()) {
            sendMessage(currentUserId, conversation.getId(), SendMessageRequest.builder()
                    .content(request.getInitialMessage().trim())
                    .type("TEXT")
                    .build());
        }

        List<ConversationParticipant> participants = conversationRepository.findParticipantsByConversationId(conversation.getId());
        ConversationParticipant other = null;
        for (ConversationParticipant p : participants) {
            if (!currentUserId.equals(p.getUserId())) {
                other = p;
                break;
            }
        }

        ParticipantDto otherDto = null;
        if (other != null) {
            String displayName = other.getUserName();
            if (displayName == null || displayName.isBlank()) {
                displayName = "Người dùng " + other.getUserId();
            }
            otherDto = ParticipantDto.builder()
                    .userId(other.getUserId())
                    .name(displayName)
                    .role(other.getRole())
                    .avatarUrl(other.getUserAvatar())
                    .build();
        }

        long unread = chatMessageRepository.countUnreadMessages(conversation.getId(), currentUserId);

        return ConversationSummaryDto.builder()
                .id(conversation.getId())
                .roomId(conversation.getRoomId())
                .roomTitle(conversation.getRoomTitle())
                .lastMessageContent(conversation.getLastMessageContent())
                .lastMessageTime(conversation.getLastMessageTime())
                .unreadCount(unread)
                .otherParticipant(otherDto)
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }

    @Transactional
    public List<ChatMessageDto> getMessages(String currentUserId, String conversationId, int limit) {
        verifyParticipant(currentUserId, conversationId);

        // Mark as read
        markAsRead(currentUserId, conversationId);

        List<ChatMessage> messages = chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId, limit);
        List<ConversationParticipant> participants = conversationRepository.findParticipantsByConversationId(conversationId);
        Map<String, String> namesByUserId = new HashMap<>();
        for (ConversationParticipant cp : participants) {
            if (cp.getUserName() != null && !cp.getUserName().isBlank()) {
                namesByUserId.put(cp.getUserId(), cp.getUserName());
            } else {
                namesByUserId.put(cp.getUserId(), cp.getUserId());
            }
        }

        return messages.stream().map(m -> ChatMessageDto.builder()
                .id(m.getId())
                .conversationId(m.getConversationId())
                .senderId(m.getSenderId())
                .senderName(namesByUserId.getOrDefault(m.getSenderId(), m.getSenderId()))
                .content(m.getContent())
                .type(m.getType())
                .isRead(m.isRead())
                .isMine(currentUserId.equals(m.getSenderId()))
                .createdAt(m.getCreatedAt())
                .build()
        ).toList();
    }

    @Transactional
    public ChatMessageDto sendMessage(String currentUserId, String conversationId, SendMessageRequest request) {
        verifyParticipant(currentUserId, conversationId);

        LocalDateTime now = LocalDateTime.now();
        String messageId = "msg-" + UUID.randomUUID().toString().substring(0, 8);

        ChatMessage msg = ChatMessage.builder()
                .id(messageId)
                .conversationId(conversationId)
                .senderId(currentUserId)
                .content(request.getContent().trim())
                .type(request.getType() != null ? request.getType() : "TEXT")
                .read(false)
                .createdAt(now)
                .build();

        ChatMessage saved = chatMessageRepository.save(msg);

        // Update conversation summary
        conversationRepository.updateLastMessage(conversationId, saved.getContent(), now);
        conversationRepository.updateLastRead(conversationId, currentUserId, now);

        List<ConversationParticipant> participants = conversationRepository.findParticipantsByConversationId(conversationId);
        String senderName = currentUserId;
        for (ConversationParticipant p : participants) {
            if (currentUserId.equals(p.getUserId())) {
                if (p.getUserName() != null && !p.getUserName().isBlank()) {
                    senderName = p.getUserName();
                }
                break;
            }
        }

        return ChatMessageDto.builder()
                .id(saved.getId())
                .conversationId(saved.getConversationId())
                .senderId(saved.getSenderId())
                .senderName(senderName)
                .content(saved.getContent())
                .type(saved.getType())
                .isRead(false)
                .isMine(true)
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Transactional
    public void markAsRead(String currentUserId, String conversationId) {
        verifyParticipant(currentUserId, conversationId);
        LocalDateTime now = LocalDateTime.now();
        conversationRepository.updateLastRead(conversationId, currentUserId, now);
        chatMessageRepository.markMessagesAsRead(conversationId, currentUserId);
    }

    private void verifyParticipant(String currentUserId, String conversationId) {
        boolean isMember = conversationRepository.findParticipant(conversationId, currentUserId).isPresent();
        if (!isMember) {
            throw new AccessDeniedException("Bạn không có quyền truy cập vào cuộc trò chuyện này.");
        }
    }
}
