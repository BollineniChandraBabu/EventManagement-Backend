package com.familywishes.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ChatSocketDtos {
  public record RealtimeSendMessageRequest(
      @NotNull Long senderId, @NotNull Long receiverId, @NotBlank String messageText) {}

  public record MarkSeenRequest(
      @NotNull Long conversationId, @NotNull Long viewerId, @NotNull Long otherUserId) {}

  public record PresenceEventRequest(@NotNull Long userId, boolean online) {}

  public record TypingEventRequest(@NotNull Long conversationId, @NotNull Long userId, boolean typing) {}
}
