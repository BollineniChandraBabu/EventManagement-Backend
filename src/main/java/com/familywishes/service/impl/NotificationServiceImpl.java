package com.familywishes.service.impl;

import com.familywishes.dto.CommonDtos.PagedResponse;
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
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
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

  private final TaskScheduler scheduler = new ConcurrentTaskScheduler();
  // Store scheduled tasks
  private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

  @Value("${scheduler.time-zone:Asia/Kolkata}")
  private String schedulerTimeZone;

  @Override
  public NotificationResponse create(NotificationRequest request) {
    String actor = currentActor();
    validateScheduleWindow(request.scheduledFrom(), request.scheduledTo());
    Notification saved =
        notificationRepository.save(
            Notification.builder()
                .title(request.title())
                .message(request.message())
                .canSendEmail(Boolean.TRUE.equals(request.canSendEmail()))
                .scheduledFrom(request.scheduledFrom())
                .scheduledTo(request.scheduledTo())
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
    notification.setScheduledFrom(request.scheduledFrom());
    notification.setScheduledTo(request.scheduledTo());
    notification.setUpdatedBy(currentActor());
    validateScheduleWindow(notification.getScheduledFrom(), notification.getScheduledTo());
    return toResponse(notificationRepository.save(notification));
  }

  @Override
  public NotificationResponse getById(Long id) {
    return toResponse(getEntity(id));
  }

  @Override
  public PagedResponse<NotificationResponse> getAll(
      int page, int size, String searchKey, String sortBy, String sortDir) {
    String normalizedSortBy = resolveSortBy(sortBy);
    Sort sort =
        "asc".equalsIgnoreCase(sortDir)
            ? Sort.by(normalizedSortBy).ascending()
            : Sort.by(normalizedSortBy).descending();
    var notificationPage =
        notificationRepository.findAllBySearchKey(
            searchKey == null ? "" : searchKey.trim(), PageRequest.of(page, size, sort));

    return new PagedResponse<>(
        notificationPage.getContent().stream().map(this::toResponse).toList(),
        notificationPage.getNumber(),
        notificationPage.getSize(),
        notificationPage.getTotalElements(),
        notificationPage.getTotalPages(),
        notificationPage.hasNext(),
        notificationPage.hasPrevious());
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
                cancelNotification(previous.getId());
              }
            });
    Notification notification = getEntity(id);
    validateScheduleWindow(notification.getScheduledFrom(), notification.getScheduledTo());
    LocalDateTime now = LocalDateTime.now(ZoneId.of(schedulerTimeZone));
    LocalDateTime effectivePublishTime =
        notification.getScheduledFrom() != null ? notification.getScheduledFrom() : now;
    notification.setPublished(true);
    notification.setPublishedAt(effectivePublishTime);
    notification.setUpdatedBy(actor);
    Notification saved = notificationRepository.save(notification);
    NotificationResponse response = toResponse(saved);

    if (effectivePublishTime.isAfter(now)) {
      pushNotificationToQueue(response);
      if (Objects.nonNull(response.scheduledTo())) {
        pushUnPublishNotificationToQueue(response);
      }
      return response;
    }

    sendNotificationEmailIfEnabled(saved, response);
    notificationRealtimePublisher.publishNotification(response);
    return response;
  }

  private void pushNotificationToQueue(NotificationResponse response) {
    Runnable task =
        () -> {
          notificationRealtimePublisher.publishNotification(response);
          System.out.println("Notification Published: " + response.id());
        };
    LocalDateTime scheduledTime = response.scheduledFrom();
    Date triggerTime = Date.from(scheduledTime.atZone(ZoneId.of(schedulerTimeZone)).toInstant());
    ScheduledFuture<?> future = scheduler.schedule(task, triggerTime);
    scheduledTasks.put(response.id() + "Publish", future);
  }

  private void pushUnPublishNotificationToQueue(NotificationResponse response) {
    Runnable task =
        () -> {
          Notification notification = getEntity(response.id());
          if (Boolean.TRUE.equals(notification.getPublished())) {
            notification.setPublished(false);
            notificationRepository.save(notification);
            notificationRealtimePublisher.publishNotification(
                    new NotificationResponse(
                            null, null, null, null, null, null, null, null, null, null, null, null));
            System.out.println("Notification UnPublished: " + response.id());
          }
        };
    LocalDateTime scheduledTime = response.scheduledTo();
    Date triggerTime = Date.from(scheduledTime.atZone(ZoneId.of(schedulerTimeZone)).toInstant());
    ScheduledFuture<?> future = scheduler.schedule(task, triggerTime);
    scheduledTasks.put(response.id() + "UnPublish", future);
  }

  private void cancelNotification(Long notificationId) {
    String publishedNotificationId = notificationId + "Publish";
    String unPublishedNotificationId = notificationId + "UnPublish";
    ScheduledFuture<?> publishFuture = scheduledTasks.get(publishedNotificationId);
    ScheduledFuture<?> unPublishFuture = scheduledTasks.get(unPublishedNotificationId);
    if (publishFuture != null) {
      publishFuture.cancel(false);
      scheduledTasks.remove(publishedNotificationId);
    }
    if (unPublishFuture != null) {
      unPublishFuture.cancel(false);
      scheduledTasks.remove(unPublishedNotificationId);
    }
    System.out.println("Notification Cancelled");
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
    cancelNotification(id);
    notificationRealtimePublisher.publishNotification(
        new NotificationResponse(
            null, null, null, null, null, null, null, null, null, null, null, null));
    return response;
  }

  @Override
  public NotificationResponse getPublished() {
    LocalDateTime now = LocalDateTime.now(ZoneId.of(schedulerTimeZone));
    Notification published =
        notificationRepository
            .findFirstByPublishedTrueOrderByPublishedAtDesc()
            .filter(notification -> !notification.getPublishedAt().isAfter(now))
            .filter(
                notification ->
                    notification.getScheduledTo() == null
                        || !notification.getScheduledTo().isBefore(now))
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
        .forEach(
            email -> gmailEmailService.sendEmailWithAttachments(email, subject, html, null, null));
  }

  private void validateScheduleWindow(LocalDateTime scheduledFrom, LocalDateTime scheduledTo) {
    if (scheduledFrom != null && scheduledTo != null && scheduledTo.isBefore(scheduledFrom)) {
      throw new BadRequestException("Scheduled to time must be after scheduled from time");
    }
  }

  private String escapeHtml(String value) {
    return value == null
        ? ""
        : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }


  private String resolveSortBy(String sortBy) {
    if (sortBy == null || sortBy.isBlank()) {
      return "createdAt";
    }
    return switch (sortBy.trim()) {
      case "id",
          "title",
          "published",
          "canSendEmail",
          "scheduledFrom",
          "scheduledTo",
          "publishedAt",
          "createdBy",
          "updatedBy",
          "createdAt",
          "updatedAt" -> sortBy.trim();
      default -> "createdAt";
    };
  }

  private NotificationResponse toResponse(Notification n) {
    return new NotificationResponse(
        n.getId(),
        n.getTitle(),
        n.getMessage(),
        n.getPublished(),
        n.getCanSendEmail(),
        n.getScheduledFrom(),
        n.getScheduledTo(),
        n.getPublishedAt(),
        n.getCreatedBy(),
        n.getUpdatedBy(),
        n.getCreatedAt(),
        n.getUpdatedAt());
  }
}
