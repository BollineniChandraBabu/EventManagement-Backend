package com.familywishes.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.familywishes.entity.Circle;
import com.familywishes.entity.User;
import com.familywishes.entity.enums.CircleStatus;
import com.familywishes.entity.enums.EmailType;
import com.familywishes.service.AuthService;
import com.familywishes.service.GmailEmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CircleRequestNotifierTest {

  @Mock private AuthService authService;
  @Mock private GmailEmailService emailService;

  @InjectMocks private CircleRequestNotifier notifier;

  private static User user(Long id, String name, String email) {
    return User.builder().id(id).name(name).email(email).build();
  }

  private Circle circle(User requester, User addressee) {
    return Circle.builder()
        .id(7L)
        .requester(requester)
        .addressee(addressee)
        .status(CircleStatus.PENDING)
        .build();
  }

  @Test
  void newRequestEmailsTheAddresseeWithAOneClickLink() {
    ReflectionTestUtils.setField(notifier, "circleLoginUrl", "https://app.test/circles");
    User requester = user(1L, "Ravi", "ravi@x.com");
    User addressee = user(2L, "Asha", "asha@x.com");
    when(authService.issueDirectLoginToken(2L)).thenReturn("tok-123");

    notifier.notifyNewRequest(circle(requester, addressee), requester);

    verify(emailService)
        .sendEmailWithAttachments(
            eq("asha@x.com"),
            contains("wants to add you"),
            contains("tok-123"),
            eq(null),
            eq(null),
            eq(EmailType.CIRCLE_REQUEST));
  }

  @Test
  void tokenFailureStillSendsThePlainLink() {
    ReflectionTestUtils.setField(notifier, "circleLoginUrl", "https://app.test/circles");
    User requester = user(1L, "Ravi", "ravi@x.com");
    User addressee = user(2L, "Asha", "asha@x.com");
    when(authService.issueDirectLoginToken(2L)).thenThrow(new RuntimeException("no token"));

    notifier.notifyNewRequest(circle(requester, addressee), requester);

    verify(emailService)
        .sendEmailWithAttachments(
            eq("asha@x.com"),
            any(String.class),
            contains("https://app.test/circles"),
            eq(null),
            eq(null),
            eq(EmailType.CIRCLE_REQUEST));
  }

  @Test
  void reminderNamesTheRequesterAndIsSent() {
    ReflectionTestUtils.setField(notifier, "circleLoginUrl", "https://app.test/circles");
    User requester = user(1L, "Ravi", "ravi@x.com");
    User addressee = user(2L, "Asha", "asha@x.com");
    when(authService.issueDirectLoginToken(2L)).thenReturn("tok-123");

    notifier.notifyPendingReminder(circle(requester, addressee));

    verify(emailService)
        .sendEmailWithAttachments(
            eq("asha@x.com"),
            contains("Ravi"),
            contains("still waiting"),
            eq(null),
            eq(null),
            eq(EmailType.CIRCLE_REQUEST));
  }

  @Test
  void addresseeWithoutEmailIsSkipped() {
    User requester = user(1L, "Ravi", "ravi@x.com");
    User addressee = user(2L, "Asha", "  ");

    notifier.notifyNewRequest(circle(requester, addressee), requester);

    verify(emailService, never())
        .sendEmailWithAttachments(any(), any(), any(), any(), any(), any());
  }

  @Test
  void acceptedRequestEmailsTheRequesterWithAOneClickLink() {
    ReflectionTestUtils.setField(notifier, "circleLoginUrl", "https://app.test/circles");
    User requester = user(1L, "Ravi", "ravi@x.com");
    User addressee = user(2L, "Asha", "asha@x.com");
    when(authService.issueDirectLoginToken(1L)).thenReturn("tok-req");

    notifier.notifyRequestAccepted(circle(requester, addressee));

    verify(emailService)
        .sendEmailWithAttachments(
            eq("ravi@x.com"),
            contains("accepted your circle request"),
            contains("tok-req"),
            eq(null),
            eq(null),
            eq(EmailType.CIRCLE_REQUEST));
  }

  @Test
  void acceptedRequestWithoutRequesterEmailIsSkipped() {
    User requester = user(1L, "Ravi", "  ");
    User addressee = user(2L, "Asha", "asha@x.com");

    notifier.notifyRequestAccepted(circle(requester, addressee));

    verify(emailService, never())
        .sendEmailWithAttachments(any(), any(), any(), any(), any(), any());
  }

  @Test
  void namesAreEscapedSoTheyCannotInjectMarkup() {
    ReflectionTestUtils.setField(notifier, "circleLoginUrl", "https://app.test/circles");
    User requester = user(1L, "<script>alert(1)</script>", "ravi@x.com");
    User addressee = user(2L, "Asha", "asha@x.com");
    when(authService.issueDirectLoginToken(2L)).thenReturn("tok-123");

    notifier.notifyNewRequest(circle(requester, addressee), requester);

    verify(emailService)
        .sendEmailWithAttachments(
            eq("asha@x.com"),
            any(String.class),
            org.mockito.ArgumentMatchers.argThat(
                body -> body.contains("&lt;script&gt;") && !body.contains("<script>")),
            eq(null),
            eq(null),
            eq(EmailType.CIRCLE_REQUEST));
  }

  @Test
  void mailFailureIsSwallowed() {
    ReflectionTestUtils.setField(notifier, "circleLoginUrl", "https://app.test/circles");
    User requester = user(1L, "Ravi", "ravi@x.com");
    User addressee = user(2L, "Asha", "asha@x.com");
    when(authService.issueDirectLoginToken(2L)).thenReturn("tok-123");
    doThrow(new RuntimeException("smtp down"))
        .when(emailService)
        .sendEmailWithAttachments(any(), any(), any(), any(), any(), any());

    notifier.notifyNewRequest(circle(requester, addressee), requester);
  }
}
