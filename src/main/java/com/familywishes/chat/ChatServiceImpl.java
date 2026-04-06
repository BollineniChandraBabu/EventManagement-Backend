package com.familywishes.chat;

import static com.familywishes.chat.ChatDtos.*;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.entity.User;
import com.familywishes.exception.BadRequestException;
import com.familywishes.exception.NotFoundException;
import com.familywishes.repository.UserRepository;
import com.familywishes.service.impl.SupabaseStorageService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
                    u.getId(), u.getName(), u.getEmail(), !u.isDeleted() && u.isActive(), u.isOnline(), u.getLastSeenAt()))
        .toList();
  }

  @Override
  public PagedResponse<ChatUserResponse> listAvailableUsers(
      int page, int size, String searchKey, String sortBy, String sortDir) {
    User me = currentUser();
    Sort sort = resolveUserSort(sortBy, sortDir);
    var pageResult =
        userRepository.findChatUsers(
            me.getId(),
            false,
            searchKey == null ? "" : searchKey.trim(),
            PageRequest.of(Math.max(page, 0), Math.max(size, 1), sort));

    return new PagedResponse<>(
        pageResult.getContent().stream()
            .map(
                u ->
                    new ChatUserResponse(
                        u.getId(), u.getName(), u.getEmail(), !u.isDeleted() && u.isActive(), u.isOnline(), u.getLastSeenAt()))
            .toList(),
        pageResult.getNumber(),
        pageResult.getSize(),
        pageResult.getTotalElements(),
        pageResult.getTotalPages(),
        pageResult.hasNext(),
        pageResult.hasPrevious());
  }

  @Override
  public List<ChatUserResponse> listActiveUsers() {
    User me = currentUser();
    return userRepository.findAll().stream()
        .filter(u -> !u.isDeleted() && u.isActive() && u.isOnline() && !u.getId().equals(me.getId()))
        .map(
            u ->
                new ChatUserResponse(
                    u.getId(), u.getName(), u.getEmail(), !u.isDeleted() && u.isActive(), u.isOnline(), u.getLastSeenAt()))
        .toList();
  }

  @Override
  public PagedResponse<ChatUserResponse> listActiveUsers(
      int page, int size, String searchKey, String sortBy, String sortDir) {
    User me = currentUser();
    Sort sort = resolveUserSort(sortBy, sortDir);
    var pageResult =
        userRepository.findChatUsers(
            me.getId(),
            true,
            searchKey == null ? "" : searchKey.trim(),
            PageRequest.of(Math.max(page, 0), Math.max(size, 1), sort));

    return new PagedResponse<>(
        pageResult.getContent().stream()
            .map(
                u ->
                    new ChatUserResponse(
                        u.getId(), u.getName(), u.getEmail(), !u.isDeleted() && u.isActive(), u.isOnline(), u.getLastSeenAt()))
            .toList(),
        pageResult.getNumber(),
        pageResult.getSize(),
        pageResult.getTotalElements(),
        pageResult.getTotalPages(),
        pageResult.hasNext(),
        pageResult.hasPrevious());
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
    String encryptedMessage = request.encryptedMessage() == null ? null : request.encryptedMessage().trim();
    if (encryptedMessage != null && encryptedMessage.isBlank()) {
      encryptedMessage = null;
    }
    if (text.isBlank() && encryptedMessage == null && (attachment == null || attachment.isEmpty())) {
      throw new BadRequestException("Message text, encrypted payload or attachment is required");
    }
    String messageType = resolveMessageType(request.messageType(), attachment, encryptedMessage != null);

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
            request.replyToMessageId(),
            text,
            encryptedMessage,
            normalizeNullable(request.encryptionAlgorithm()),
            normalizeNullable(request.encryptionKeyId()),
            messageType,
            request.voiceDurationSeconds(),
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
            request.replyToMessageId(),
            text,
            encryptedMessage,
            normalizeNullable(request.encryptionAlgorithm()),
            normalizeNullable(request.encryptionKeyId()),
            messageType,
            request.voiceDurationSeconds(),
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
  public MessageResponse sendRealtimeMessage(
      Long senderId,
      Long receiverId,
      String messageText,
      String encryptedMessage,
      String encryptionAlgorithm,
      String encryptionKeyId,
      String messageType,
      Integer voiceDurationSeconds) {
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
    String normalizedEncryptedMessage = normalizeNullable(encryptedMessage);
    if (text.isBlank() && normalizedEncryptedMessage == null) {
      throw new BadRequestException("Message text or encrypted payload is required");
    }

    Long conversationId = resolveConversationId(sender.getId(), receiver.getId());
    LocalDateTime now = nowIst();
    String resolvedType = resolveMessageType(messageType, null, normalizedEncryptedMessage != null);
    Long messageId =
        chatMessageRepository.insertMessage(
            conversationId,
            sender.getId(),
            receiver.getId(),
            null,
            text,
            normalizedEncryptedMessage,
            normalizeNullable(encryptionAlgorithm),
            normalizeNullable(encryptionKeyId),
            resolvedType,
            voiceDurationSeconds,
            null,
            null,
            null,
            now);

    MessageResponse response =
        new MessageResponse(
            messageId,
            conversationId,
            sender.getId(),
            receiver.getId(),
            null,
            text,
            normalizedEncryptedMessage,
            normalizeNullable(encryptionAlgorithm),
            normalizeNullable(encryptionKeyId),
            resolvedType,
            voiceDurationSeconds,
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
    List<ChatMessageRepository.ConversationSummary> rows =
        chatMessageRepository.findConversationSummaries(me.getId());
    Map<Long, User> userById = loadUsersById(rows);
    return rows.stream().map(row -> toConversationResponse(row, userById)).toList();
  }

  @Override
  public PagedResponse<ConversationResponse> listConversations(
      int page, int size, String searchKey, String sortBy, String sortDir) {
    User me = currentUser();
    int safePage = Math.max(page, 0);
    int safeSize = Math.max(size, 1);
    String normalizedSearch = searchKey == null ? "" : searchKey.trim().toLowerCase();
    if (normalizedSearch.isEmpty()) {
      List<ChatMessageRepository.ConversationSummary> pageRows =
          chatMessageRepository.findConversationSummaries(me.getId(), safePage, safeSize, "");
      Map<Long, User> userById = loadUsersById(pageRows);
      List<ConversationResponse> content =
          pageRows.stream()
              .map(row -> toConversationResponse(row, userById))
              .filter(row -> row.otherUserName() != null)
              .toList();
      long total = chatMessageRepository.countConversationSummaries(me.getId(), "");
      int totalPages = (int) Math.ceil((double) total / safeSize);
      return new PagedResponse<>(
          content, safePage, safeSize, total, totalPages, safePage + 1 < totalPages, safePage > 0);
    }

    List<ChatMessageRepository.ConversationSummary> rows =
        chatMessageRepository.findConversationSummaries(me.getId());
    Map<Long, User> userById = loadUsersById(rows);

    List<ConversationResponse> filtered =
        rows.stream()
            .map(row -> toConversationResponse(row, userById))
            .filter(row -> row.otherUserName() != null)
            .filter(row -> matchesConversationSearch(row, normalizedSearch))
            .toList();

    int fromIndex = Math.min(safePage * safeSize, filtered.size());
    int toIndex = Math.min(fromIndex + safeSize, filtered.size());
    List<ConversationResponse> content = filtered.subList(fromIndex, toIndex);
    long total = filtered.size();
    int totalPages = (int) Math.ceil((double) total / safeSize);
    return new PagedResponse<>(
        content, safePage, safeSize, total, totalPages, safePage + 1 < totalPages, safePage > 0);
  }

  private Map<Long, User> loadUsersById(List<ChatMessageRepository.ConversationSummary> rows) {
    Set<Long> otherUserIds =
        rows.stream().map(ChatMessageRepository.ConversationSummary::otherUserId).collect(Collectors.toSet());
    if (otherUserIds.isEmpty()) {
      return Map.of();
    }
    return userRepository.findByIdInAndDeletedFalse(otherUserIds).stream()
        .collect(Collectors.toMap(User::getId, u -> u));
  }

  private ConversationResponse toConversationResponse(
      ChatMessageRepository.ConversationSummary row, Map<Long, User> userById) {
    User other = userById.get(row.otherUserId());
    return new ConversationResponse(
        row.conversationId(),
        row.otherUserId(),
        other == null ? null : other.getName(),
        other == null ? null : other.getEmail(),
        other != null && other.isOnline(),
        other == null ? null : other.getLastSeenAt(),
        row.messageText(),
        row.sentAt(),
        row.seenAt(),
        row.unreadCount());
  }

  private boolean matchesConversationSearch(ConversationResponse row, String normalizedSearch) {
    if (normalizedSearch.isEmpty()) {
      return true;
    }
    return containsIgnoreCase(row.otherUserName(), normalizedSearch)
        || containsIgnoreCase(row.otherUserEmail(), normalizedSearch)
        || containsIgnoreCase(row.lastMessage(), normalizedSearch);
  }

  private boolean containsIgnoreCase(String value, String needleLower) {
    return value != null && value.toLowerCase().contains(needleLower);
  }

  private String normalizeNullable(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private String resolveMessageType(
      String requestedType, MultipartFile attachment, boolean encryptedPayloadPresent) {
    String normalizedRequested = normalizeNullable(requestedType);
    if (normalizedRequested != null) {
      return normalizedRequested.toUpperCase();
    }
    if (attachment != null && !attachment.isEmpty()) {
      String contentType = attachment.getContentType();
      if (contentType != null && contentType.toLowerCase().startsWith("audio/")) {
        return "VOICE";
      }
      return "FILE";
    }
    if (encryptedPayloadPresent) {
      return "ENCRYPTED";
    }
    return "TEXT";
  }

  @Override
  public GlobalMessagePageResponse listAllMessages(int page, int size, String searchKey) {
    List<GlobalMessageResponse> rows = chatMessageRepository.findGlobalMessages(page, size, searchKey);
    Set<Long> userIds =
        rows.stream()
            .flatMap(r -> java.util.stream.Stream.of(r.senderId(), r.receiverId()))
            .collect(Collectors.toSet());
    Map<Long, User> users =
        userRepository.findByIdInAndDeletedFalse(userIds).stream()
            .collect(Collectors.toMap(User::getId, u -> u));

    List<GlobalMessageResponse> items =
        rows.stream()
            .map(
                r -> {
                  User sender = users.get(r.senderId());
                  User receiver = users.get(r.receiverId());
                  return new GlobalMessageResponse(
                      r.messageId(),
                      r.conversationId(),
                      r.senderId(),
                      sender == null ? null : sender.getName(),
                      r.receiverId(),
                      receiver == null ? null : receiver.getName(),
                      r.messageText(),
                      r.attachmentFileName(),
                      r.sentAt(),
                      r.seenAt());
                })
            .toList();
    return new GlobalMessagePageResponse(items, page, size, items.size() == size);
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
    }else {
      chatMessageRepository.removeAllReactions(deleted.messageId(),otherUserId);
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

  @Override
  public MessageReactionsResponse listReactions(Long messageId) {
    User me = currentUser();
    ensureCanAccessMessage(me.getId(), messageId);
    return toMessageReactionsResponse(messageId, me.getId());
  }

  @Override
  @Transactional
  public MessageReactionsResponse reactToMessage(Long messageId, MessageReactionRequest request) {
    User me = currentUser();
    ensureCanAccessMessage(me.getId(), messageId);
    String emoji = request.emoji() == null ? "" : request.emoji().trim();
    if (emoji.isBlank()) {
      throw new BadRequestException("Emoji is required");
    }
    chatMessageRepository.addReaction(messageId, me.getId(), emoji, nowIst());
    return toMessageReactionsResponse(messageId, me.getId());
  }

  @Override
  @Transactional
  public MessageReactionsResponse likeMessage(Long messageId, MessageReactionRequest request) {
    User me = currentUser();
    ensureCanAccessMessage(me.getId(), messageId);
    String emoji = request.emoji() == null ? "" : request.emoji().trim();
    if (emoji.isBlank()) {
      throw new BadRequestException("Emoji is required");
    }
    chatMessageRepository.addReaction(messageId, me.getId(), emoji, nowIst());
    return toMessageReactionsResponse(messageId, me.getId());
  }

  @Override
  @Transactional
  public MessageReactionsResponse unlikeMessage(Long messageId, MessageReactionRequest request) {
    User me = currentUser();
    ensureCanAccessMessage(me.getId(), messageId);
    String emoji = request.emoji() == null ? "" : request.emoji().trim();
    if (emoji.isBlank()) {
      throw new BadRequestException("Emoji is required");
    }
    chatMessageRepository.removeReaction(messageId, me.getId(), emoji);
    return toMessageReactionsResponse(messageId, me.getId());
  }

  private void ensureCanAccessMessage(Long userId, Long messageId) {
    if (chatMessageRepository.findMessageMetaForParticipant(messageId, userId) == null) {
      throw new NotFoundException("Message not found");
    }
  }

  private MessageReactionsResponse toMessageReactionsResponse(Long messageId, Long me) {
    List<MessageReactionResponse> reactions =
        chatMessageRepository.findReactions(messageId, me).stream()
            .map(r -> new MessageReactionResponse(r.emoji(), r.count(), r.mine()))
            .toList();
    return new MessageReactionsResponse(messageId, reactions);
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

  private Sort resolveUserSort(String sortBy, String sortDir) {
    String normalizedSortBy =
        switch (sortBy) {
          case "email", "online", "lastSeenAt", "id", "createdAt", "updatedAt", "name" -> sortBy;
          case "userId" -> "id";
          default -> "name";
        };
    return "desc".equalsIgnoreCase(sortDir)
        ? Sort.by(normalizedSortBy).descending()
        : Sort.by(normalizedSortBy).ascending();
  }

  private <T> PagedResponse<T> page(
      List<T> source, int page, int size, Predicate<T> filter, Comparator<T> comparator) {
    List<T> filteredSorted = source.stream().filter(filter).sorted(comparator).toList();
    int safePage = Math.max(page, 0);
    int safeSize = Math.max(size, 1);
    int fromIndex = Math.min(safePage * safeSize, filteredSorted.size());
    int toIndex = Math.min(fromIndex + safeSize, filteredSorted.size());
    List<T> content = filteredSorted.subList(fromIndex, toIndex);
    int totalPages = (int) Math.ceil((double) filteredSorted.size() / safeSize);

    return new PagedResponse<>(
        content,
        safePage,
        safeSize,
        filteredSorted.size(),
        totalPages,
        safePage + 1 < totalPages,
        safePage > 0 && totalPages > 0);
  }
}
