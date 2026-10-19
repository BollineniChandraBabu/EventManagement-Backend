package com.familywishes.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.familywishes.dto.EmailDtos.SendEmailNowRequest;
import com.familywishes.dto.EmailImage;
import com.familywishes.entity.EmailLog;
import com.familywishes.entity.EventTypeSeed;
import com.familywishes.entity.User;
import com.familywishes.entity.enums.EmailStatus;
import com.familywishes.entity.enums.EmailType;
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
  @Mock private EmailImageService emailImageService;

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
                EventTypeSeed.builder()
                    .id(2L)
                    .code("ANNIVERSARY")
                    .displayName("Anniversary")
                    .build()));
    when(emailLogRepository.save(any(EmailLog.class)))
        .thenAnswer(
            invocation -> {
              EmailLog log = invocation.getArgument(0);
              log.setId(100L);
              return log;
            });
    doNothing()
        .when(serviceSpy)
        .sendEmailWithAttachments(
            eq("user@example.com"), eq("Subject"), any(String.class), eq(100L), eq(null));

    serviceSpy.sendEmailNow(request);

    verify(emailLogRepository).save(any(EmailLog.class));
    verify(serviceSpy)
        .sendEmailWithAttachments(
            eq("user@example.com"), eq("Subject"), any(String.class), eq(100L), eq(null));
  }

  private static EmailLog logWithImage(EmailType type, String recipientEmail, String imageKey) {
    return EmailLog.builder()
        .id(7L)
        .emailType(type)
        .status(EmailStatus.SENT)
        .imageUrl(imageKey)
        .recipientUser(User.builder().email(recipientEmail).build())
        .build();
  }

  @Test
  void getImageShouldReturnTheImageToItsRecipient() {
    EmailImage image = new EmailImage(new byte[] {1, 2, 3, 4}, "image/png");
    when(emailLogRepository.findById(7L))
        .thenReturn(Optional.of(logWithImage(EmailType.EVENT, "me@example.com", "key-7")));
    when(emailImageService.load(7L, "key-7")).thenReturn(image);

    assertSame(image, gmailEmailService.getImage(7L, "ME@example.com", false));
  }

  @Test
  void getImageShouldLetAdminsOpenAnyEmailIncludingSensitiveOnes() {
    EmailImage image = new EmailImage(new byte[] {1, 2, 3, 4}, "image/png");
    when(emailLogRepository.findById(7L))
        .thenReturn(Optional.of(logWithImage(EmailType.OTP, "someone@example.com", "key-7")));
    when(emailImageService.load(7L, "key-7")).thenReturn(image);

    assertSame(image, gmailEmailService.getImage(7L, "admin@example.com", true));
  }

  @Test
  void getImageShouldHideOtherPeoplesEmailsFromNonAdmins() {
    when(emailLogRepository.findById(7L))
        .thenReturn(Optional.of(logWithImage(EmailType.EVENT, "other@example.com", "key-7")));

    assertThrows(
        NotFoundException.class, () -> gmailEmailService.getImage(7L, "me@example.com", false));
    verify(emailImageService, never()).load(any(), any());
  }

  @Test
  void getImageShouldHideOtpAndPasswordResetEmailsFromNonAdmins() {
    when(emailLogRepository.findById(7L))
        .thenReturn(Optional.of(logWithImage(EmailType.FORGOT_PASSWORD, "me@example.com", "k")));

    assertThrows(
        NotFoundException.class, () -> gmailEmailService.getImage(7L, "me@example.com", false));
    verify(emailImageService, never()).load(any(), any());
  }

  @Test
  void getImageShouldFailWhenTheEmailHasNoImageOrItCannotBeLoaded() {
    when(emailLogRepository.findById(7L))
        .thenReturn(Optional.of(logWithImage(EmailType.EVENT, "me@example.com", null)));
    assertThrows(
        NotFoundException.class, () -> gmailEmailService.getImage(7L, "me@example.com", false));

    when(emailLogRepository.findById(8L))
        .thenReturn(Optional.of(logWithImage(EmailType.EVENT, "me@example.com", "key-8")));
    when(emailImageService.load(8L, "key-8")).thenReturn(null);
    assertThrows(
        NotFoundException.class, () -> gmailEmailService.getImage(8L, "me@example.com", false));
  }

  @Test
  void statusListsOnlyTellWhetherAnImageExistsInsteadOfDownloadingIt() {
    when(emailLogRepository.findById(7L))
        .thenReturn(Optional.of(logWithImage(EmailType.EVENT, "me@example.com", "key-7")));

    var status = gmailEmailService.getStatusById(7L, "me@example.com");

    assertEquals(true, status.hasImage());
    verify(emailImageService, never()).load(any(), any());
  }

  @Test
  void deleteEmailStatusShouldDropTheCachedImage() {
    when(emailLogRepository.findById(7L))
        .thenReturn(Optional.of(logWithImage(EmailType.EVENT, "me@example.com", null)));

    gmailEmailService.deleteEmailStatus(7L);

    verify(emailLogRepository).deleteById(7L);
    verify(emailImageService).evict(7L);
  }
}
