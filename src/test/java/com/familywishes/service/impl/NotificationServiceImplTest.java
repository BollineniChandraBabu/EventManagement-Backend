package com.familywishes.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.familywishes.dto.NotificationDtos.NotificationRequest;
import com.familywishes.entity.Notification;
import com.familywishes.exception.BadRequestException;
import com.familywishes.repository.NotificationRepository;
import com.familywishes.repository.UserRepository;
import com.familywishes.service.GmailEmailService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

  @Mock private NotificationRepository notificationRepository;
  @Mock private NotificationRealtimePublisher notificationRealtimePublisher;
  @Mock private UserRepository userRepository;
  @Mock private GmailEmailService gmailEmailService;

  @InjectMocks private NotificationServiceImpl notificationService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(notificationService, "schedulerTimeZone", "UTC");
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("admin@x.com", null));
  }

  @org.junit.jupiter.api.AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void createRejectsAScheduleInThePast() {
    NotificationRequest request =
        new NotificationRequest(
            "Title",
            "Message",
            false,
            LocalDateTime.now().minusMinutes(5),
            LocalDateTime.now().plusHours(1));

    assertThrows(BadRequestException.class, () -> notificationService.create(request));
    verify(notificationRepository, never()).save(any());
  }

  @Test
  void createRejectsAnEndTimeInThePast() {
    NotificationRequest request =
        new NotificationRequest(
            "Title",
            "Message",
            false,
            LocalDateTime.now().plusHours(1),
            LocalDateTime.now().minusMinutes(1));

    assertThrows(BadRequestException.class, () -> notificationService.create(request));
    verify(notificationRepository, never()).save(any());
  }

  @Test
  void createAcceptsAFutureWindow() {
    when(notificationRepository.save(any(Notification.class)))
        .thenAnswer(call -> call.getArgument(0));
    NotificationRequest request =
        new NotificationRequest(
            "Title",
            "Message",
            false,
            LocalDateTime.now().plusHours(1),
            LocalDateTime.now().plusHours(2));

    notificationService.create(request);

    verify(notificationRepository).save(any(Notification.class));
  }

  @Test
  void deleteByIdsRejectsAnEmptyList() {
    assertThrows(BadRequestException.class, () -> notificationService.deleteByIds(List.of()));
    verify(notificationRepository, never()).delete(any());
  }

  @Test
  void deleteByIdsRemovesEachNotification() {
    Notification notification =
        Notification.builder().id(3L).title("Title").message("Message").build();
    when(notificationRepository.findById(3L)).thenReturn(Optional.of(notification));

    notificationService.deleteByIds(List.of(3L, 3L));

    verify(notificationRepository).delete(notification);
  }
}
