package com.familywishes.controller;

import com.familywishes.dto.NotificationDtos.NotificationResponse;
import com.familywishes.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class PublishedNotificationController {
  private final NotificationService notificationService;

  @GetMapping("/published")
  public NotificationResponse getPublished() {
    return notificationService.getPublished();
  }
}
