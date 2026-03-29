package com.familywishes.chat;

import static com.familywishes.chat.ChatDtos.*;

import com.familywishes.entity.User;
import com.familywishes.exception.BadRequestException;
import com.familywishes.exception.NotFoundException;
import com.familywishes.repository.UserRepository;
import com.familywishes.service.impl.SupabaseStorageService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

  private final ChatMessageRepository chatMessageRepository;
  private final UserRepository userRepository;
  private final SupabaseStorageService storageService;
  private final ChatRealtimePublisher realtimePublisher;

  @Override
  public List<ChatUserResponse> listAvailableUsers() {
    User me = currentUser();
    return userRepository.findAll().stream()
        .filter(u -> !u.isDeleted() && u.isActive() && !u.getId().equals(me.getId()))
        .map(
            u ->
                new ChatUserResponse(
                    u.getId(), u.getName(), u.getEmail(), u.isOnline(), u.getLastSeenAt()))
        .toList();
  }

  @Override
  public List<ChatUserResponse> listActiveUsers() {
    User me = currentUser();
    return userRepository.findAll().stream()
        .filter(u -> !u.isDeleted() && u.isActive() && u.isOnline() && !u.getId().equals(me.getId()))
        .map(
            u ->
                new ChatUserResponse(
                    u.getId(), u.getName(), u.getEmail(), u.isOnline(), u.getLastSeenAt()))
        .toList();
  }

  @Override
  @Transactional
  public MessageResponse sendMessage(SendMessageRequest request, MultipartFile attachment) {
    User me = currentUser();
    User receiver =
        userRepository
            .findById(request.receiverId())
            .filter(u -> !u.isDeleted() && u.isActive())
            .orElseThrow(() -> new NotFoundException("Receiver not found"));

    String text = request.messageText() == null ? "" : request.messageText().trim();
    if (text.isBlank() && (attachment == null || attachment.isEmpty())) {
      throw new BadRequestException("Message text or attachment is required");
    }

    Long conversationId = resolveConversationId(me.getId(), receiver.getId());
    String attachmentKey = null;
    String fileName = null;
    String contentType = null;

    if (attachment != null && !attachment.isEmpty()) {
      attachmentKey = storageService.uploadChatAttachment(attachment);
      fileName = attachment.getOriginalFilename();
      contentType = attachment.getContentType();
    }

    LocalDateTime now = nowIst();
    Long messageId =
        chatMessageRepository.insertMessage(
            conversationId,
            me.getId(),
            receiver.getId(),
            text,
            attachmentKey,
            fileName,
            contentType,
            now);

    MessageResponse response =
        new MessageResponse(
            messageId,
            conversationId,
            me.getId(),
            receiver.getId(),
            text,
            attachmentKey,
            fileName,
            contentType,
            now,
            null,
            true);

    realtimePublisher.publishMessage(conversationId, response);
    return response;
  }

  @Override
  @Transactional
  public MessageResponse sendRealtimeTextMessage(Long senderId, Long receiverId, String messageText) {
    User sender =
        userRepository
            .findById(senderId)
            .filter(u -> !u.isDeleted() && u.isActive())
            .orElseThrow(() -> new NotFoundException("Sender not found"));

    User receiver =
        userRepository
            .findById(receiverId)
            .filter(u -> !u.isDeleted() && u.isActive())
            .orElseThrow(() -> new NotFoundException("Receiver not found"));

    String text = messageText == null ? "" : messageText.trim();
    if (text.isBlank()) {
      throw new BadRequestException("Message text is required");
    }

    Long conversationId = resolveConversationId(sender.getId(), receiver.getId());
    LocalDateTime now = nowIst();
    Long messageId =
        chatMessageRepository.insertMessage(
            conversationId, sender.getId(), receiver.getId(), text, null, null, null, now);

    MessageResponse response =
        new MessageResponse(
            messageId,
            conversationId,
            sender.getId(),
            receiver.getId(),
            text,
            null,
            null,
            null,
            now,
            null,
            true);

    realtimePublisher.publishMessage(conversationId, response);
    return response;
  }

  @Override
  @Transactional
  public MessagePageResponse getConversationMessages(
      Long otherUserId, int page, int size, boolean markSeen) {
    User me = currentUser();
    User other =
        userRepository.findById(otherUserId).orElseThrow(() -> new NotFoundException("User not found"));

    Long conversationId = resolveConversationId(me.getId(), other.getId());

    if (markSeen) {
      chatMessageRepository.markConversationSeen(conversationId, me.getId(), nowIst());
    }

    List<MessageResponse> messages =
        chatMessageRepository.findConversationMessages(conversationId, page, size, me.getId());

    return new MessagePageResponse(messages, page, size, messages.size() == size);
  }

  @Override
  public List<ConversationResponse> listConversations() {
    User me = currentUser();

    return chatMessageRepository.findConversationSummaries(me.getId()).stream()
        .map(
            row -> {
              Long otherId = ((Number) row.get("other_user_id")).longValue();
              User other = userRepository.findById(otherId).orElseThrow();
              return new ConversationResponse(
                  ((Number) row.get("conversation_id")).longValue(),
                  otherId,
                  other.getName(),
                  other.getEmail(),
                  other.isOnline(),
                  other.getLastSeenAt(),
                  (String) row.get("message_text"),
                  toLocalDateTime(row.get("sent_at")),
                  toLocalDateTime(row.get("seen_at")),
                  ((Number) row.get("unread_count")).longValue());
            })
        .toList();
  }

  @Override
  public GlobalMessagePageResponse listAllMessages(int page, int size, String searchKey) {
    List<GlobalMessageResponse> items = chatMessageRepository.findGlobalMessages(page, size, searchKey);

    List<GlobalMessageResponse> enriched =
        items.stream()
            .map(
                item ->
                    new GlobalMessageResponse(
                        item.messageId(),
                        item.conversationId(),
                        item.senderId(),
                        userRepository.findById(item.senderId()).map(User::getName).orElse("Unknown"),
                        item.receiverId(),
                        userRepository.findById(item.receiverId()).map(User::getName).orElse("Unknown"),
                        item.messageText(),
                        item.attachmentFileName(),
                        item.sentAt(),
                        item.seenAt()))
            .toList();

    return new GlobalMessagePageResponse(enriched, page, size, enriched.size() == size);
  }

  @Override
  public void heartbeat() {
    User me = currentUser();
    updatePresence(me, true);
  }

  @Override
  public void markOffline() {
    User me = currentUser();
    updatePresence(me, false);
  }

  @Override
  public void heartbeat(Long userId) {
    User user =
        userRepository
            .findById(userId)
            .filter(u -> !u.isDeleted() && u.isActive())
            .orElseThrow(() -> new NotFoundException("User not found"));
    updatePresence(user, true);
  }

  @Override
  public void markOffline(Long userId) {
    User user =
        userRepository
            .findById(userId)
            .filter(u -> !u.isDeleted() && u.isActive())
            .orElseThrow(() -> new NotFoundException("User not found"));
    updatePresence(user, false);
  }

  @Override
  @Transactional
  public void markConversationSeen(Long viewerId, Long otherUserId) {
    User viewer =
        userRepository
            .findById(viewerId)
            .filter(u -> !u.isDeleted() && u.isActive())
            .orElseThrow(() -> new NotFoundException("Viewer not found"));
    User other =
        userRepository
            .findById(otherUserId)
            .filter(u -> !u.isDeleted() && u.isActive())
            .orElseThrow(() -> new NotFoundException("User not found"));

    Long conversationId = resolveConversationId(viewer.getId(), other.getId());
    chatMessageRepository.markConversationSeen(conversationId, viewer.getId(), nowIst());
  }

  @Override
  public byte[] downloadAttachment(Long messageId) {
    User me = currentUser();
    String key = chatMessageRepository.findAttachmentKeyByMessageIdForUser(messageId, me.getId());
    if (key == null || key.isBlank()) {
      throw new NotFoundException("Attachment not found");
    }

    byte[] data = storageService.downloadImage(key);
    if (data == null || data.length == 0) {
      throw new NotFoundException("Attachment not found");
    }
    return data;
  }

  @Override
  @Transactional
  public DeleteMessageResponse deleteLastSentMessage(Long otherUserId) {
    User me = currentUser();
    User other =
        userRepository
            .findById(otherUserId)
            .filter(u -> !u.isDeleted() && u.isActive())
            .orElseThrow(() -> new NotFoundException("User not found"));

    Long conversationId = resolveConversationId(me.getId(), other.getId());
    ChatMessageRepository.MessageMeta lastSent =
        chatMessageRepository.findLastSentMessageMeta(conversationId, me.getId());
    if (lastSent == null) {
      throw new NotFoundException("No sent message found to delete");
    }

    LocalDateTime deletableFrom = nowIst().minusMinutes(15);
    if (lastSent.sentAt() == null || lastSent.sentAt().isBefore(deletableFrom)) {
      throw new BadRequestException("Message can be deleted only within 15 minutes of sending");
    }

    LocalDateTime deletedAt = nowIst();
    DeleteMessageResponse deleted =
        chatMessageRepository.deleteLastSentMessage(
            conversationId, me.getId(), deletableFrom, deletedAt);
    if (deleted == null) {
      throw new NotFoundException("No sent message found to delete");
    }

    realtimePublisher.publishMessageDeleted(deleted.conversationId(), deleted.messageId(), me.getId());
    return deleted;
  }

  @Override
  @Transactional
  public MessageResponse editMessage(Long messageId, EditMessageRequest request) {
    User me = currentUser();
    String text = request.messageText() == null ? "" : request.messageText().trim();
    if (text.isBlank()) {
      throw new BadRequestException("Message text is required");
    }

    ChatMessageRepository.MessageMeta meta =
        chatMessageRepository.findMessageMetaForParticipant(messageId, me.getId());
    if (meta == null) {
      throw new NotFoundException("Message not found");
    }
    if (!me.getId().equals(meta.senderId())) {
      throw new BadRequestException("Only sender can edit this message");
    }

    LocalDateTime editableFrom = nowIst().minusMinutes(15);
    if (meta.sentAt() == null || meta.sentAt().isBefore(editableFrom)) {
      throw new BadRequestException("Message can be edited only within 15 minutes of sending");
    }

    MessageResponse updated = chatMessageRepository.updateMessageText(messageId, me.getId(), text, me.getId());
    if (updated == null) {
      throw new NotFoundException("Message not found");
    }

    realtimePublisher.publishMessageEdited(updated, me.getId());
    return updated;
  }

  private Long resolveConversationId(Long user1, Long user2) {
    long a = Math.min(user1, user2);
    long b = Math.max(user1, user2);
    Long existing = chatMessageRepository.findConversationId(a, b);
    if (existing != null) {
      return existing;
    }
    return chatMessageRepository.createConversation(a, b, nowIst());
  }

  private LocalDateTime toLocalDateTime(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof java.sql.Timestamp timestamp) {
      return timestamp.toLocalDateTime();
    }
    if (value instanceof LocalDateTime localDateTime) {
      return localDateTime;
    }
    return ((java.sql.Date) value).toLocalDate().atStartOfDay();
  }

  private LocalDateTime nowIst() {
    return LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
  }

  private void updatePresence(User user, boolean online) {
    user.setOnline(online);
    user.setLastSeenAt(nowIst());
    userRepository.save(user);
    realtimePublisher.publishPresence(user.getId(), online, String.valueOf(user.getLastSeenAt()));
  }

  private User currentUser() {
    var authentication =
        org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new NotFoundException("Authenticated user not found");
    }
    return userRepository
        .findByEmailAndDeletedFalse(authentication.getName())
        .orElseThrow(() -> new NotFoundException("User not found"));
  }
}
