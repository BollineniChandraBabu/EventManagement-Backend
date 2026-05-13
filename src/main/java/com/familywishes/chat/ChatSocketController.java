package com.familywishes.chat;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatSocketController {

  private final ChatService chatService;
  private final SimpMessagingTemplate messagingTemplate;

  @MessageMapping("/chat.send")
  public void send(@Payload @Valid ChatSocketDtos.RealtimeSendMessageRequest payload) {
    chatService.sendRealtimeMessage(
        payload.senderId(),
        payload.receiverId(),
        payload.messageText(),
        payload.encryptedMessage(),
        payload.encryptionAlgorithm(),
        payload.encryptionKeyId(),
        payload.messageType(),
        payload.voiceDurationSeconds());
  }

  @MessageMapping("/chat.seen")
  public void seen(@Payload @Valid ChatSocketDtos.MarkSeenRequest payload) {
    chatService.markConversationSeen(payload.viewerId(), payload.otherUserId());
    messagingTemplate.convertAndSend(
        "/topic/chat/" + payload.conversationId(),
        java.util.Map.of(
            "type",
            "SEEN",
            "conversationId",
            payload.conversationId(),
            "viewerId",
            payload.viewerId()));
  }

  @MessageMapping("/chat.presence")
  public void presence(@Payload @Valid ChatSocketDtos.PresenceEventRequest payload) {
    if (payload.online()) {
      chatService.heartbeat(payload.userId());
    } else {
      chatService.markOffline(payload.userId());
    }
  }

  @MessageMapping("/chat.typing")
  public void typing(@Payload @Valid ChatSocketDtos.TypingEventRequest payload) {
    messagingTemplate.convertAndSend(
        "/topic/chat/" + payload.conversationId(),
        java.util.Map.of(
            "type", "TYPING",
            "conversationId", payload.conversationId(),
            "userId", payload.userId(),
            "typing", payload.typing()));
  }
}
