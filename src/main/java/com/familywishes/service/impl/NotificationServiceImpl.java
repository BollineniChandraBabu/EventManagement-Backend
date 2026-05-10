package com.familywishes.service.impl;

import com.familywishes.dto.NotificationDtos.NotificationRequest;
import com.familywishes.dto.NotificationDtos.NotificationResponse;
import com.familywishes.entity.Notification;
import com.familywishes.exception.BadRequestException;
import com.familywishes.exception.NotFoundException;
import com.familywishes.repository.NotificationRepository;
import com.familywishes.repository.UserRepository;
import com.familywishes.service.GmailEmailService;
import com.familywishes.service.NotificationService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
  private final NotificationRepository notificationRepository;
  private final NotificationRealtimePublisher notificationRealtimePublisher;
  private final UserRepository userRepository;
  private final GmailEmailService gmailEmailService;

  @Override
  public NotificationResponse create(NotificationRequest request) {
    String actor = currentActor();
    Notification saved =
        notificationRepository.save(
            Notification.builder()
                .title(request.title())
                                .message(request.message())
                .canSendEmail(Boolean.TRUE.equals(request.canSendEmail()))
                .createdBy(actor)
                .updatedBy(actor)
                .build());
    return toResponse(saved);
  }

  @Override
  public NotificationResponse update(Long id, NotificationRequest request) {
    Notification notification = getEntity(id);
    ensureNotPublished(notification, "Published notifications cannot be edited");
    notification.setTitle(request.title());
    notification.setMessage(request.message());
    notification.setCanSendEmail(Boolean.TRUE.equals(request.canSendEmail()));
    notification.setUpdatedBy(currentActor());
    return toResponse(notificationRepository.save(notification));
  }

  @Override
  public NotificationResponse getById(Long id) {
    return toResponse(getEntity(id));
  }

  @Override
  public List<NotificationResponse> getAll() {
    return notificationRepository.findAll().stream().map(this::toResponse).toList();
  }

  @Override
  public void delete(Long id) {
    Notification notification = getEntity(id);
    ensureNotPublished(notification, "Published notifications cannot be deleted");
    notificationRepository.delete(notification);
  }

  @Override
  @Transactional
  public NotificationResponse publish(Long id) {
    String actor = currentActor();
    notificationRepository
        .findFirstByPublishedTrueOrderByPublishedAtDesc()
        .ifPresent(
            previous -> {
              if (!previous.getId().equals(id)) {
                previous.setPublished(false);
                previous.setUpdatedBy(actor);
                notificationRepository.save(previous);
              }
            });

    Notification notification = getEntity(id);
    notification.setPublished(true);
    notification.setPublishedAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
    notification.setUpdatedBy(actor);
    Notification saved = notificationRepository.save(notification);
    NotificationResponse response = toResponse(saved);
    sendNotificationEmailIfEnabled(saved, response);
    notificationRealtimePublisher.publishNotification(response);
    return response;
  }

  @Override
  public NotificationResponse unpublish(Long id) {
    Notification notification = getEntity(id);
    if (!Boolean.TRUE.equals(notification.getPublished())) {
      throw new BadRequestException("Notification is already unpublished");
    }
    notification.setPublished(false);
    notification.setUpdatedBy(currentActor());
    Notification saved = notificationRepository.save(notification);
    NotificationResponse response = toResponse(saved);
    sendNotificationEmailIfEnabled(saved, response);
    notificationRealtimePublisher.publishNotification(null);
    return response;
  }

  @Override
  public NotificationResponse getPublished() {
    Notification published =
        notificationRepository
            .findFirstByPublishedTrueOrderByPublishedAtDesc()
            .orElseThrow(() -> new NotFoundException("No published notification found"));
    return toResponse(published);
  }

  private Notification getEntity(Long id) {
    return notificationRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Notification not found with id: " + id));
  }

  private String currentActor() {
    return SecurityContextHolder.getContext().getAuthentication().getName();
  }

  private void ensureNotPublished(Notification notification, String message) {
    if (Boolean.TRUE.equals(notification.getPublished())) {
      throw new BadRequestException(message);
    }
  }


  private void sendNotificationEmailIfEnabled(
      Notification notification, NotificationResponse response) {
    if (!Boolean.TRUE.equals(notification.getCanSendEmail())) {
      return;
    }

    String subject = "Golden Greetings Notification: " + response.title();
    String html =
        "<h3>"
            + escapeHtml(response.title())
            + "</h3><p>"
            + escapeHtml(response.message())
            + "</p>";

    userRepository.findAll().stream()
        .filter(user -> !user.isDeleted())
        .map(user -> user.getEmail())
        .filter(email -> email != null && !email.isBlank())
        .forEach(email -> gmailEmailService.sendEmailWithAttachments(email, subject, html, null, null));
  }

  private String escapeHtml(String value) {
    return value == null
        ? ""
        : value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
  }
  private NotificationResponse toResponse(Notification n) {
    return new NotificationResponse(
        n.getId(),
        n.getTitle(),
        n.getMessage(),
        n.getPublished(),
        n.getCanSendEmail(),
        n.getPublishedAt(),
        n.getCreatedBy(),
        n.getUpdatedBy(),
        n.getCreatedAt(),
        n.getUpdatedAt());
  }
}
