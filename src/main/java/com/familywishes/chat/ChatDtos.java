package com.familywishes.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public class ChatDtos {

  public record ChatUserResponse(
      Long userId,
      String name,
      String email,
      boolean active,
      boolean online,
      LocalDateTime lastSeenAt,
      String profilePictureUrl) {}

  public record SendMessageRequest(
      @NotNull Long receiverId,
      String messageText,
      String encryptedMessage,
      String encryptionAlgorithm,
      String encryptionKeyId,
      String messageType,
      Integer voiceDurationSeconds,
      Long replyToMessageId) {}

  public record EditMessageRequest(@NotBlank String messageText) {}

  public record MessageResponse(
      Long messageId,
      Long conversationId,
      Long senderId,
      Long receiverId,
      Long replyToMessageId,
      String messageText,
      String encryptedMessage,
      String encryptionAlgorithm,
      String encryptionKeyId,
      String messageType,
      Integer voiceDurationSeconds,
      String attachmentKey,
      String attachmentFileName,
      String attachmentContentType,
      LocalDateTime sentAt,
      LocalDateTime seenAt,
      boolean mine) {}

  public record MessageReactionRequest(@NotBlank String emoji) {}

  public record MessageReactionResponse(String emoji, long count, boolean mine) {}

  public record MessageReactionsResponse(Long messageId, List<MessageReactionResponse> reactions) {}

  public record ConversationResponse(
      Long conversationId,
      Long otherUserId,
      String otherUserName,
      String otherUserEmail,
      boolean otherUserOnline,
      LocalDateTime otherUserLastSeenAt,
      String otherUserProfilePictureUrl,
      String lastMessage,
      LocalDateTime lastMessageAt,
      LocalDateTime lastSeenMessageAt,
      long unreadCount) {}

  public record MessagePageResponse(List<MessageResponse> items, int page, int size, boolean hasNext) {}

  public record GlobalMessageResponse(
      Long messageId,
      Long conversationId,
      Long senderId,
      String senderName,
      Long receiverId,
      String receiverName,
      String messageText,
      String attachmentFileName,
      LocalDateTime sentAt,
      LocalDateTime seenAt) {}

  public record GlobalMessagePageResponse(
      List<GlobalMessageResponse> items, int page, int size, boolean hasNext) {}

  public record DeleteMessageResponse(Long messageId, Long conversationId, LocalDateTime deletedAt) {}
}
