package com.familywishes.chat;

import com.familywishes.chat.ChatDtos.MessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatRealtimePublisher {
  private final SimpMessagingTemplate messagingTemplate;

  public void publishMessage(Long conversationId, MessageResponse message) {
    messagingTemplate.convertAndSend("/topic/chat/" + conversationId, message);
  }

  public void publishPresence(Long userId, boolean online, String lastSeenAt) {
    messagingTemplate.convertAndSend(
        "/topic/presence",
        java.util.Map.of("userId", userId, "online", online, "lastSeenAt", lastSeenAt));
  }

  public void publishMessageDeleted(Long conversationId, Long messageId, Long deletedByUserId) {
    messagingTemplate.convertAndSend(
        "/topic/chat/" + conversationId,
        java.util.Map.of(
            "type", "MESSAGE_DELETED",
            "conversationId", conversationId,
            "messageId", messageId,
            "deletedByUserId", deletedByUserId));
  }

  public void publishMessageEdited(MessageResponse message, Long editedByUserId) {
    messagingTemplate.convertAndSend(
        "/topic/chat/" + message.conversationId(),
        java.util.Map.of("type", "MESSAGE_EDITED", "message", message, "editedByUserId", editedByUserId));
  }
}
