package com.familywishes.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.familywishes.dto.EmailDtos.SendEmailNowRequest;
import com.familywishes.entity.EmailLog;
import com.familywishes.entity.EventTypeSeed;
import com.familywishes.entity.User;
import com.familywishes.exception.NotFoundException;
import com.familywishes.repository.EmailLogRepository;
import com.familywishes.repository.EventTypeSeedRepository;
import com.familywishes.repository.UserRepository;
import com.google.api.services.gmail.Gmail;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GmailEmailServiceImplTest {

  @Mock private EmailLogRepository emailLogRepository;
  @Mock private UserRepository userRepository;
  @Mock private EventTypeSeedRepository eventTypeSeedRepository;
  @Mock private Gmail gmail;

  @InjectMocks private GmailEmailServiceImpl gmailEmailService;

  @Test
  void sendEmailNowShouldFailWhenUserMissing() {
    SendEmailNowRequest request =
        new SendEmailNowRequest("missing@example.com", "Subject", "Body", "ANNIVERSARY");
    when(userRepository.findByEmailAndDeletedFalse("missing@example.com"))
        .thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> gmailEmailService.sendEmailNow(request));
  }

  @Test
  void sendEmailNowShouldFailWhenEventTypeMissing() {
    SendEmailNowRequest request =
        new SendEmailNowRequest("user@example.com", "Subject", "Body", "UNKNOWN");

    when(userRepository.findByEmailAndDeletedFalse("user@example.com"))
        .thenReturn(Optional.of(User.builder().id(1L).email("user@example.com").build()));
    when(eventTypeSeedRepository.findByCodeAndActiveTrue("UNKNOWN")).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> gmailEmailService.sendEmailNow(request));
  }

  @Test
  void sendEmailNowShouldCreateEventLogAndDispatchEmail() {
    GmailEmailServiceImpl serviceSpy = spy(gmailEmailService);

    SendEmailNowRequest request =
        new SendEmailNowRequest("user@example.com", "Subject", "Body", "ANNIVERSARY");

    when(userRepository.findByEmailAndDeletedFalse("user@example.com"))
        .thenReturn(Optional.of(User.builder().id(1L).email("user@example.com").build()));
    when(eventTypeSeedRepository.findByCodeAndActiveTrue("ANNIVERSARY"))
        .thenReturn(
            Optional.of(
                EventTypeSeed.builder().id(2L).code("ANNIVERSARY").displayName("Anniversary").build()));
    when(emailLogRepository.save(any(EmailLog.class)))
        .thenAnswer(
            invocation -> {
              EmailLog log = invocation.getArgument(0);
              log.setId(100L);
              return log;
            });
    doNothing()
        .when(serviceSpy)
        .sendEmailWithAttachments(eq("user@example.com"), eq("Subject"), any(String.class), eq(100L), eq(null));

    serviceSpy.sendEmailNow(request);

    verify(emailLogRepository).save(any(EmailLog.class));
    verify(serviceSpy)
        .sendEmailWithAttachments(eq("user@example.com"), eq("Subject"), any(String.class), eq(100L), eq(null));
  }
}
