package com.familywishes.service.impl;

import com.familywishes.dto.NotificationDtos.NotificationResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationRealtimePublisher {
  private final SimpMessagingTemplate messagingTemplate;

  public void publishNotification(NotificationResponse notification) {
    messagingTemplate.convertAndSend(
        "/topic/notifications",
        Map.of("type", "NOTIFICATION_PUBLISHED", "notification", notification));
  }
}
