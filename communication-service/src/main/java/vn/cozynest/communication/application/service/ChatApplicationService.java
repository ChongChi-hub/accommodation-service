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

    public static String resolveCanonicalUserId(String userId) {
        if (userId == null || userId.isBlank()) return userId;
        String trimmed = userId.trim();
        if ("2122472122".equals(trimmed) || "u-landlord-2122472122".equals(trimmed)
                || "1".equals(trimmed) || "u-landlord-01".equals(trimmed) || "u-landlord-1".equals(trimmed)) {
            return "u-huy-01";
        }
        if ("2".equals(trimmed) || "u-landlord-02".equals(trimmed) || "u-landlord-2".equals(trimmed)) {
            return "u-hao-01";
        }
        return trimmed;
    }

    public static Set<String> getUserAliases(String userId) {
        Set<String> set = new HashSet<>();
        if (userId == null || userId.isBlank()) return set;
        String trimmed = userId.trim();
        set.add(trimmed);
        String canonical = resolveCanonicalUserId(trimmed);
        set.add(canonical);
        if ("u-huy-01".equals(canonical)) {
            set.add("2122472122");
            set.add("u-landlord-2122472122");
            set.add("1");
            set.add("u-landlord-01");
            set.add("u-landlord-1");
        } else if ("u-hao-01".equals(canonical)) {
            set.add("2");
            set.add("u-landlord-02");
            set.add("u-landlord-2");
        }
        return set;
    }

    @Transactional(readOnly = true)
    public List<ConversationSummaryDto> getConversations(String currentUserId) {
        Set<String> aliases = getUserAliases(currentUserId);
        List<Conversation> conversations = new ArrayList<>();
        Set<String> seenConvIds = new HashSet<>();
        for (String alias : aliases) {
            for (Conversation c : conversationRepository.findByUserId(alias)) {
                if (seenConvIds.add(c.getId())) {
                    conversations.add(c);
                }
            }
        }
        conversations.sort((a, b) -> {
            LocalDateTime ta = a.getLastMessageTime() != null ? a.getLastMessageTime() : a.getCreatedAt();
            LocalDateTime tb = b.getLastMessageTime() != null ? b.getLastMessageTime() : b.getCreatedAt();
            return tb.compareTo(ta);
        });

        List<ConversationSummaryDto> summaries = new ArrayList<>();

        for (Conversation c : conversations) {
            List<ConversationParticipant> participants = conversationRepository.findParticipantsByConversationId(c.getId());
            ConversationParticipant other = null;
            for (ConversationParticipant p : participants) {
                if (!aliases.contains(p.getUserId())) {
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

            long unreadCount = 0;
            for (String alias : aliases) {
                unreadCount = Math.max(unreadCount, chatMessageRepository.countUnreadMessages(c.getId(), alias));
            }

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
        String canonicalRecipientId = resolveCanonicalUserId(request.getRecipientId());
        String canonicalCurrentUserId = resolveCanonicalUserId(currentUserId);

        if (canonicalCurrentUserId.equals(canonicalRecipientId)) {
            throw new IllegalArgumentException("Không thể tự nhắn tin cho chính mình.");
        }

        Set<String> currentAliases = getUserAliases(currentUserId);
        Set<String> recipientAliases = getUserAliases(request.getRecipientId());

        Optional<Conversation> existing = Optional.empty();
        for (String cAlias : currentAliases) {
            for (String rAlias : recipientAliases) {
                existing = conversationRepository.findByParticipants(cAlias, rAlias, request.getRoomId());
                if (existing.isPresent()) break;
            }
            if (existing.isPresent()) break;
        }

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
                senderName = "Người dùng " + canonicalCurrentUserId;
            }
            ConversationParticipant senderParticipant = ConversationParticipant.builder()
                    .id("cp-" + UUID.randomUUID().toString().substring(0, 8))
                    .conversationId(convId)
                    .userId(canonicalCurrentUserId)
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
                recName = "Người dùng " + canonicalRecipientId;
            }
            ConversationParticipant recParticipant = ConversationParticipant.builder()
                    .id("cp-" + UUID.randomUUID().toString().substring(0, 8))
                    .conversationId(convId)
                    .userId(canonicalRecipientId)
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
            if (!currentAliases.contains(p.getUserId())) {
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

        long unread = chatMessageRepository.countUnreadMessages(conversation.getId(), canonicalCurrentUserId);

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

        Set<String> aliases = getUserAliases(currentUserId);
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
                .isMine(aliases.contains(m.getSenderId()))
                .createdAt(m.getCreatedAt())
                .build()
        ).toList();
    }

    @Transactional
    public ChatMessageDto sendMessage(String currentUserId, String conversationId, SendMessageRequest request) {
        verifyParticipant(currentUserId, conversationId);

        LocalDateTime now = LocalDateTime.now();
        String messageId = "msg-" + UUID.randomUUID().toString().substring(0, 8);
        String canonicalUserId = resolveCanonicalUserId(currentUserId);

        ChatMessage msg = ChatMessage.builder()
                .id(messageId)
                .conversationId(conversationId)
                .senderId(canonicalUserId)
                .content(request.getContent().trim())
                .type(request.getType() != null ? request.getType() : "TEXT")
                .read(false)
                .createdAt(now)
                .build();

        ChatMessage saved = chatMessageRepository.save(msg);

        // Update conversation summary
        conversationRepository.updateLastMessage(conversationId, saved.getContent(), now);
        conversationRepository.updateLastRead(conversationId, canonicalUserId, now);

        List<ConversationParticipant> participants = conversationRepository.findParticipantsByConversationId(conversationId);
        String senderName = canonicalUserId;
        Set<String> aliases = getUserAliases(currentUserId);
        for (ConversationParticipant p : participants) {
            if (aliases.contains(p.getUserId())) {
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
        String canonical = resolveCanonicalUserId(currentUserId);
        conversationRepository.updateLastRead(conversationId, canonical, now);
        Set<String> aliases = getUserAliases(currentUserId);
        for (String alias : aliases) {
            chatMessageRepository.markMessagesAsRead(conversationId, alias);
        }
    }

    private void verifyParticipant(String currentUserId, String conversationId) {
        Set<String> aliases = getUserAliases(currentUserId);
        List<ConversationParticipant> participants = conversationRepository.findParticipantsByConversationId(conversationId);
        boolean isMember = participants.stream().anyMatch(p -> aliases.contains(p.getUserId()));
        if (!isMember) {
            throw new AccessDeniedException("Bạn không có quyền truy cập vào cuộc trò chuyện này.");
        }
    }
}
