package com.familywishes.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.familywishes.entity.MessageStatus;
import com.familywishes.entity.User;
import com.familywishes.entity.enums.EmailStatus;
import com.familywishes.entity.enums.EmailType;
import com.familywishes.repository.EmailLogRepository;
import com.familywishes.repository.EventRepository;
import com.familywishes.repository.IGMessageLogRepository;
import com.familywishes.repository.InstagramUserRepository;
import com.familywishes.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

  @Mock private UserRepository userRepository;
  @Mock private EventRepository eventRepository;
  @Mock private EmailLogRepository emailLogRepository;
  @Mock private IGMessageLogRepository igMessageLogRepository;
  @Mock private InstagramUserRepository instagramUserRepository;

  @InjectMocks private DashboardServiceImpl dashboardService;

  @Test
  void instaDashboardShouldUseConnectedInstagramUserCountForAdmin() {
    when(instagramUserRepository.countByInstagramUserIdIsNotNullAndInstagramUserIdNot(""))
        .thenReturn(3L);
    when(eventRepository.countByEventDateGreaterThanEqualAndActiveTrue(any())).thenReturn(7L);
    when(igMessageLogRepository.countByStatusAndCreatedAtBetween(
            eq(MessageStatus.SENT), any(), any()))
        .thenReturn(5L);
    when(igMessageLogRepository.countByStatus(MessageStatus.FAILED)).thenReturn(2L);
    var response = dashboardService.getIGDashboard("admin@test.com", true);
    assertEquals(3L, response.totalUsers());
    assertEquals(7L, response.upcomingEvents());
    assertEquals(5L, response.emailsSentToday());
    assertEquals(2L, response.failedEmails());
  }

  @Test
  void otpDashboardShouldReturnSensitiveCountsOnly() {
    when(emailLogRepository.countByStatusAndSentAtGreaterThanEqualAndSentAtLessThanAndEmailTypeIn(
            eq(EmailStatus.SENT), any(), any(), eq(List.of(EmailType.OTP))))
        .thenReturn(4L);
    when(emailLogRepository.countByStatusAndEmailTypeIn(
            eq(EmailStatus.FAILED), eq(List.of(EmailType.OTP))))
        .thenReturn(1L);
    var response = dashboardService.getOtpDashboard();
    assertEquals(0L, response.totalUsers());
    assertEquals(4L, response.emailsSentToday());
    assertEquals(1L, response.failedEmails());
  }

  @Test
  void userDashboardShouldBeUserScoped() {
    when(userRepository.findByEmailAndDeletedFalse("user@test.com"))
        .thenReturn(Optional.of(User.builder().id(10L).build()));
    when(eventRepository.countByUser_IdAndEventDateGreaterThanEqualAndActiveTrue(eq(10L), any()))
        .thenReturn(6L);
    when(emailLogRepository
            .countByRecipientUserEmailAndStatusAndSentAtGreaterThanEqualAndSentAtLessThanAndEmailTypeNotIn(
                eq("user@test.com"), eq(EmailStatus.SENT), any(), any(), any()))
        .thenReturn(2L);
    when(emailLogRepository.countByRecipientUserEmailAndStatusAndEmailTypeNotIn(
            eq("user@test.com"), eq(EmailStatus.FAILED), any()))
        .thenReturn(1L);
    var response = dashboardService.getDashboard("user@test.com", false);
    assertEquals(1L, response.totalUsers());
    assertEquals(6L, response.upcomingEvents());
    assertEquals(2L, response.emailsSentToday());
    assertEquals(1L, response.failedEmails());
  }
}
