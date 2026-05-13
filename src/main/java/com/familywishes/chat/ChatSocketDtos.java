package com.familywishes.chat;

import jakarta.validation.constraints.NotNull;

public class ChatSocketDtos {
  public record RealtimeSendMessageRequest(
      @NotNull Long senderId,
      @NotNull Long receiverId,
      String messageText,
      String encryptedMessage,
      String encryptionAlgorithm,
      String encryptionKeyId,
      String messageType,
      Integer voiceDurationSeconds) {}

  public record MarkSeenRequest(
      @NotNull Long conversationId, @NotNull Long viewerId, @NotNull Long otherUserId) {}

  public record PresenceEventRequest(@NotNull Long userId, boolean online) {}

  public record TypingEventRequest(
      @NotNull Long conversationId, @NotNull Long userId, boolean typing) {}
}
