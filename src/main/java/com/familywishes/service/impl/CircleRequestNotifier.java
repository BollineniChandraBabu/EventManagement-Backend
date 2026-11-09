package com.familywishes.service.impl;

import com.familywishes.entity.Circle;
import com.familywishes.entity.User;
import com.familywishes.entity.enums.EmailType;
import com.familywishes.service.AuthService;
import com.familywishes.service.GmailEmailService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Composes and sends the circle-request emails: the initial "someone added you" note and the daily
 * reminder while a request is still unanswered. The one-click sign-in link is issued on a
 * best-effort basis so a failure to mint a token never stops the email; a mail failure is swallowed
 * so it never blocks the circle request itself.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CircleRequestNotifier {

  private final AuthService authService;
  private final GmailEmailService emailService;

  @Value(
      "${app.circle.request.login-url:${app.ui.base-url:https://chandu-goldengreetings.web.app}/circles}")
  private String circleLoginUrl;

  public void notifyNewRequest(Circle circle, User requester) {
    User addressee = circle.getAddressee();
    if (!hasEmail(addressee)) {
      log.warn("Skipping circle request email: addressee has no email (circle {})", circle.getId());
      return;
    }
    String subject = "You have a new circle request";
    String html = buildBody(addressee, requester, buildLink(addressee));
    send(circle, addressee, subject, html);
  }

  /** Daily nudge for a request that is still pending, so it does not sit unanswered forever. */
  public void notifyPendingReminder(Circle circle) {
    User addressee = circle.getAddressee();
    if (!hasEmail(addressee)) {
      log.warn(
          "Skipping circle reminder email: addressee has no email (circle {})", circle.getId());
      return;
    }
    User requester = circle.getRequester();
    String requesterName = requester == null ? "Someone" : requester.getName();
    String subject = "Reminder: " + requesterName + " is still waiting for your circle request";
    String html = buildReminderBody(addressee, requesterName, buildLink(addressee));
    send(circle, addressee, subject, html);
  }

  private void send(Circle circle, User addressee, String subject, String html) {
    try {
      emailService.sendEmailWithAttachments(
          addressee.getEmail(), subject, html, null, null, EmailType.CIRCLE_REQUEST);
      log.info("Circle email queued for {} (circle {})", addressee.getEmail(), circle.getId());
    } catch (Exception e) {
      log.error("Circle email failed for {}", addressee.getEmail(), e);
    }
  }

  private boolean hasEmail(User user) {
    return user != null && user.getEmail() != null && !user.getEmail().isBlank();
  }

  /** Falls back to the plain circles page when a one-click sign-in token cannot be issued. */
  private String buildLink(User addressee) {
    try {
      String token = authService.issueDirectLoginToken(addressee.getId());
      if (token == null || token.isBlank()) {
        return circleLoginUrl;
      }
      return circleLoginUrl
          + "?token="
          + URLEncoder.encode(token, StandardCharsets.UTF_8)
          + "&email="
          + URLEncoder.encode(addressee.getEmail(), StandardCharsets.UTF_8);
    } catch (Exception e) {
      log.warn(
          "Could not issue a direct-login token for {}; emailing the plain circles link",
          addressee.getEmail(),
          e);
      return circleLoginUrl;
    }
  }

  private String buildBody(User addressee, User requester, String link) {
    return """
<div style='font-family:Arial,sans-serif;line-height:1.5;color:#111827'>
  <h3 style='margin-bottom:8px'>New circle request</h3>
  <p>Hi %s,</p>
  <p><b>%s</b> would like to add you to their circle.</p>
  <p><a href='%s'>Open Golden Greetings</a></p>
  <p>You will be signed in directly, so you can accept &mdash; no password needed.</p>
  <p style='color:#6b7280'>If you did not expect this, you can safely ignore this email.</p>
</div>
"""
        .formatted(addressee.getName(), requester.getName(), link);
  }

  private String buildReminderBody(User addressee, String requesterName, String link) {
    return """
<div style='font-family:Arial,sans-serif;line-height:1.5;color:#111827'>
  <h3 style='margin-bottom:8px'>Your circle request is still waiting</h3>
  <p>Hi %s,</p>
  <p><b>%s</b> sent you a circle request and it has been pending for a while.</p>
  <p><a href='%s'>Open Golden Greetings</a></p>
  <p>Accept it to start sharing wishes, or reject it if you would rather not.</p>
  <p style='color:#6b7280'>You can also ignore this email; we will stop reminding you once you respond.</p>
</div>
"""
        .formatted(addressee.getName(), requesterName, link);
  }
}
