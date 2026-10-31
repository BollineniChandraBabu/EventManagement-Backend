package com.familywishes.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.familywishes.dto.AuthDtos.ForgotPasswordRequest;
import com.familywishes.dto.AuthDtos.OtpSendRequest;
import com.familywishes.dto.EventDtos.EventRequest;
import com.familywishes.dto.EventDtos.EventResponse;
import com.familywishes.dto.FestivalDtos.FestivalWishMappingRequest;
import com.familywishes.dto.FestivalDtos.FestivalWishMappingResponse;
import com.familywishes.dto.UserDtos.UserRequest;
import com.familywishes.dto.UserDtos.UserResponse;
import com.familywishes.entity.Event;
import com.familywishes.entity.EventTypeSeed;
import com.familywishes.entity.FestivalWishMapping;
import com.familywishes.entity.Notification;
import com.familywishes.entity.OtpCode;
import com.familywishes.entity.PasswordResetToken;
import com.familywishes.entity.RelationshipSeed;
import com.familywishes.entity.SpecialEvent;
import com.familywishes.entity.User;
import com.familywishes.entity.enums.Gender;
import com.familywishes.entity.enums.Role;
import com.familywishes.exception.BadRequestException;
import com.familywishes.repository.EventRepository;
import com.familywishes.repository.EventTypeSeedRepository;
import com.familywishes.repository.FestivalWishMappingRepository;
import com.familywishes.repository.NotificationRepository;
import com.familywishes.repository.OtpCodeRepository;
import com.familywishes.repository.PasswordResetTokenRepository;
import com.familywishes.repository.SpecialEventRepository;
import com.familywishes.repository.UserRepository;
import com.familywishes.service.GmailEmailService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NewFeaturesTest {

  @Mock private EventRepository eventRepository;
  @Mock private UserRepository userRepository;
  @Mock private EventTypeSeedRepository eventTypeSeedRepository;
  @Mock private SupabaseStorageService storageService;
  @InjectMocks private EventServiceImpl eventService;

  @Mock private FestivalWishMappingRepository festivalMappingRepository;
  @Mock private SpecialEventRepository specialEventRepository;
  @InjectMocks private FestivalWishMappingServiceImpl festivalWishMappingService;

  @Mock private PasswordEncoder passwordEncoder;
  @Mock private com.familywishes.repository.RelationshipSeedRepository relationshipSeedRepository;
  @InjectMocks private UserServiceImpl userService;

  @Mock private OtpCodeRepository otpCodeRepository;
  @Mock private PasswordResetTokenRepository resetTokenRepository;
  @Mock private GmailEmailService gmailEmailService;
  @InjectMocks private AuthServiceImpl authService;

  @Mock private NotificationRepository notificationRepository;
  @InjectMocks private NotificationServiceImpl notificationService;

  @Test
  void eventCreateWithImageUploadsToStorage() {
    User user = User.builder().id(1L).name("John").email("john@example.com").build();
    EventTypeSeed eventType =
        EventTypeSeed.builder().id(1L).code("BIRTHDAY").displayName("Birthday").build();
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(eventTypeSeedRepository.findByCodeAndActiveTrue("BIRTHDAY"))
        .thenReturn(Optional.of(eventType));
    when(storageService.uploadWishImage(any())).thenReturn("events/sample-image.png");
    when(storageService.getSignedUrl("events/sample-image.png"))
        .thenReturn("https://storage.url/events/sample-image.png");
    when(eventRepository.save(any()))
        .thenAnswer(
            i -> {
              Event e = i.getArgument(0);
              e.setId(10L);
              return e;
            });

    MockMultipartFile image =
        new MockMultipartFile("image", "test.png", "image/png", new byte[] {1, 2, 3});
    EventRequest req = new EventRequest("BIRTHDAY", LocalDate.now(), false, 1L, null);
    EventResponse resp = eventService.createWithImage(req, image);

    assertEquals(10L, resp.id());
    assertEquals("https://storage.url/events/sample-image.png", resp.imageUrl());
  }

  @Test
  void festivalWishMappingStoresWebsiteLink() {
    SpecialEvent festival = SpecialEvent.builder().id(2L).eventName("Diwali").build();
    User user = User.builder().id(3L).name("Alice").build();
    when(specialEventRepository.findById(2L)).thenReturn(Optional.of(festival));
    when(userRepository.findById(3L)).thenReturn(Optional.of(user));
    when(festivalMappingRepository.findBySpecialEvent_IdAndUser_Id(2L, 3L))
        .thenReturn(Optional.empty());
    when(festivalMappingRepository.save(any()))
        .thenAnswer(
            i -> {
              FestivalWishMapping m = i.getArgument(0);
              m.setId(20L);
              return m;
            });

    FestivalWishMappingRequest req =
        new FestivalWishMappingRequest(2L, 3L, true, "Note", "https://example.com/wishes");
    FestivalWishMappingResponse resp = festivalWishMappingService.upsert(req);

    assertEquals("https://example.com/wishes", resp.websiteLink());
  }

  @Test
  void userCreateAndToggleMfa() {
    RelationshipSeed rel = RelationshipSeed.builder().id(1L).code("FRIEND").build();
    when(userRepository.save(any()))
        .thenAnswer(
            i -> {
              User u = i.getArgument(0);
              u.setId(5L);
              return u;
            });
    when(relationshipSeedRepository.findByCodeAndActiveTrue("FRIEND")).thenReturn(Optional.of(rel));

    UserRequest req =
        new UserRequest(
            "Bob",
            "bob@example.com",
            Role.ROLE_USER,
            Gender.MALE,
            LocalDate.of(1990, 1, 1),
            "FRIEND",
            true,
            true,
            true,
            null,
            true);
    UserResponse resp = userService.create(req);
    assertTrue(resp.mfaEnabled());
  }

  @Test
  void otpResendsExistingOtpAndLimitsTo5Times() {
    ReflectionTestUtils.setField(authService, "maxOtpResendAttempts", 5);
    ReflectionTestUtils.setField(authService, "schedulerTimeZone", "UTC");
    User user = User.builder().id(7L).email("user@example.com").active(true).build();
    when(userRepository.findByEmailAndDeletedFalse("user@example.com"))
        .thenReturn(Optional.of(user));

    OtpCode existingOtp =
        OtpCode.builder()
            .user(user)
            .code("123456")
            .expiresAt(LocalDateTime.now(java.time.ZoneId.of("UTC")).plusMinutes(5))
            .used(false)
            .resendAttempts(4)
            .build();
    when(otpCodeRepository.findTopByUserIdOrderByIdDesc(7L)).thenReturn(Optional.of(existingOtp));

    authService.sendOtp(new OtpSendRequest("user@example.com"));
    assertEquals(5, existingOtp.getResendAttempts());
    verify(otpCodeRepository).save(existingOtp);

    // 6th attempt should fail
    assertThrows(
        BadRequestException.class,
        () -> authService.sendOtp(new OtpSendRequest("user@example.com")));
  }

  @Test
  void forgotPasswordResendsExistingTokenAndLimitsTo5Times() {
    ReflectionTestUtils.setField(authService, "maxPasswordResetAttempts", 5);
    ReflectionTestUtils.setField(authService, "schedulerTimeZone", "UTC");
    ReflectionTestUtils.setField(authService, "passwordResetUiUrl", "http://localhost/reset");
    User user = User.builder().id(8L).name("User").email("user2@example.com").active(true).build();
    when(userRepository.findByEmailAndDeletedFalse("user2@example.com"))
        .thenReturn(Optional.of(user));

    PasswordResetToken existingToken =
        PasswordResetToken.builder()
            .user(user)
            .token("token-xyz")
            .expiresAt(LocalDateTime.now(java.time.ZoneId.of("UTC")).plusMinutes(30))
            .used(false)
            .sendAttempts(4)
            .build();
    when(resetTokenRepository.findTopByUserIdOrderByIdDesc(8L))
        .thenReturn(Optional.of(existingToken));

    authService.forgotPassword(new ForgotPasswordRequest("user2@example.com"));
    assertEquals(5, existingToken.getSendAttempts());
    verify(resetTokenRepository).save(existingToken);

    // 6th attempt should fail
    assertThrows(
        BadRequestException.class,
        () -> authService.forgotPassword(new ForgotPasswordRequest("user2@example.com")));
  }

  @Test
  void notificationPublishRequiresFutureDates() {
    org.springframework.security.core.context.SecurityContextHolder.getContext()
        .setAuthentication(
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "admin@example.com", "pass"));
    Notification notif =
        Notification.builder()
            .id(99L)
            .title("Test")
            .scheduledFrom(LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata")).minusMinutes(10))
            .build();
    when(notificationRepository.findById(99L)).thenReturn(Optional.of(notif));

    BadRequestException ex =
        assertThrows(BadRequestException.class, () -> notificationService.publish(99L));
    assertEquals("Notification can only be published to future dates", ex.getMessage());
  }
}
