package com.familywishes.service.impl;

import com.familywishes.entity.Circle;
import com.familywishes.entity.User;
import com.familywishes.entity.enums.EmailType;
import com.familywishes.service.AuthService;
import com.familywishes.service.GmailEmailService;
import com.familywishes.util.WishEmailExtras;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Composes and sends the circle-request emails: the initial "someone added you" note, the daily
 * reminder while a request is still unanswered, and the confirmation sent to the requester once the
 * request is accepted. The one-click sign-in link is issued on a best-effort basis so a failure to
 * mint a token never stops the email; a mail failure is swallowed so it never blocks the circle
 * request itself.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CircleRequestNotifier {

  private static final String FONT = "font-family:Arial,Helvetica,sans-serif;";
  private static final String ACCENT = "#6d28d9";
  private static final String TEXT = "#111827";
  private static final String MUTED = "#6b7280";

  private final AuthService authService;
  private final GmailEmailService emailService;

  @Value(
      "${app.circle.request.login-url:${app.ui.base-url:https://chandu-goldengreetings.web.app}/circles}")
  private String circleLoginUrl;

  /** Tells the addressee that someone wants to add them, with a one-tap accept link. */
  public void notifyNewRequest(Circle circle, User requester) {
    User addressee = circle.getAddressee();
    if (!hasEmail(addressee)) {
      log.warn("Skipping circle request email: addressee has no email (circle {})", circle.getId());
      return;
    }
    String requesterName = displayName(requester);
    String subject = requesterName + " wants to add you to their circle";
    String html = buildRequestBody(addressee, requesterName, buildLink(addressee));
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
    String requesterName = displayName(circle.getRequester());
    String subject = "Reminder: " + requesterName + " is still waiting for your circle request";
    String html = buildReminderBody(addressee, requesterName, buildLink(addressee));
    send(circle, addressee, subject, html);
  }

  /** Confirms to the requester that their pending circle request was accepted. */
  public void notifyRequestAccepted(Circle circle) {
    User requester = circle.getRequester();
    if (!hasEmail(requester)) {
      log.warn(
          "Skipping circle accepted email: requester has no email (circle {})", circle.getId());
      return;
    }
    String addresseeName = displayName(circle.getAddressee());
    String subject = addresseeName + " accepted your circle request";
    String html = buildAcceptedBody(requester, addresseeName, buildLink(requester));
    send(circle, requester, subject, html);
  }

  private void send(Circle circle, User recipient, String subject, String html) {
    try {
      emailService.sendEmailWithAttachments(
          recipient.getEmail(), subject, html, null, null, EmailType.CIRCLE_REQUEST);
      log.info("Circle email queued for {} (circle {})", recipient.getEmail(), circle.getId());
    } catch (Exception e) {
      log.error("Circle email failed for {}", recipient.getEmail(), e);
    }
  }

  private boolean hasEmail(User user) {
    return user != null && user.getEmail() != null && !user.getEmail().isBlank();
  }

  private String displayName(User user) {
    return user == null || user.getName() == null || user.getName().isBlank()
        ? "Someone"
        : user.getName().trim();
  }

  /** Falls back to the plain circles page when a one-click sign-in token cannot be issued. */
  private String buildLink(User recipient) {
    try {
      String token = authService.issueDirectLoginToken(recipient.getId());
      if (token == null || token.isBlank()) {
        return circleLoginUrl;
      }
      return circleLoginUrl
          + "?token="
          + URLEncoder.encode(token, StandardCharsets.UTF_8)
          + "&email="
          + URLEncoder.encode(recipient.getEmail(), StandardCharsets.UTF_8);
    } catch (Exception e) {
      log.warn(
          "Could not issue a direct-login token for {}; emailing the plain circles link",
          recipient.getEmail(),
          e);
      return circleLoginUrl;
    }
  }

  private String buildRequestBody(User addressee, String requesterName, String link) {
    return """
<div style="%sline-height:1.6;color:%s;">
  <h2 style="margin:0 0 12px;font-size:22px;font-weight:700;color:#1f2937;">You have a new circle request</h2>
  <p style="margin:0 0 12px;">Hi %s,</p>
  <p style="margin:0 0 16px;"><b>%s</b> would like to add you to their circle on Golden Greetings.</p>
  <div style="margin:0 0 16px;padding:14px 16px;background:#f9fafb;border:1px solid #e5e7eb;border-radius:10px;">
    <p style="margin:0;font-size:14px;color:#374151;">A circle lets you exchange birthday, festival and everyday wishes with people you trust.</p>
  </div>
  %s
  <p style="margin:0 0 8px;">No password needed &mdash; the button signs you in so you can accept in one tap.</p>
  <p style="margin:16px 0 0;font-size:13px;color:%s;">If you were not expecting this request, you can safely ignore this email. Nothing changes until you accept.</p>
</div>
"""
        .formatted(
            FONT,
            TEXT,
            escape(addressee.getName()),
            escape(requesterName),
            ctaButton(link, "Accept circle request"),
            MUTED);
  }

  private String buildReminderBody(User addressee, String requesterName, String link) {
    return """
<div style="%sline-height:1.6;color:%s;">
  <h2 style="margin:0 0 12px;font-size:22px;font-weight:700;color:#1f2937;">Your circle request is still waiting</h2>
  <p style="margin:0 0 12px;">Hi %s,</p>
  <p style="margin:0 0 16px;"><b>%s</b> sent you a circle request and it has been pending for a while.</p>
  %s
  <p style="margin:0 0 8px;">Accept it to start sharing wishes, or reject it if you would rather not.</p>
  <p style="margin:16px 0 0;font-size:13px;color:%s;">We will stop reminding you as soon as you respond.</p>
</div>
"""
        .formatted(
            FONT,
            TEXT,
            escape(addressee.getName()),
            escape(requesterName),
            ctaButton(link, "Review circle request"),
            MUTED);
  }

  private String buildAcceptedBody(User requester, String addresseeName, String link) {
    return """
<div style="%sline-height:1.6;color:%s;">
  <h2 style="margin:0 0 12px;font-size:22px;font-weight:700;color:#1f2937;">Your circle request was accepted</h2>
  <p style="margin:0 0 12px;">Hi %s,</p>
  <p style="margin:0 0 16px;">Good news &mdash; <b>%s</b> accepted your circle request. You are now in each other's circles.</p>
  <div style="margin:0 0 16px;padding:14px 16px;background:#f5f3ff;border:1px solid #ddd6fe;border-radius:10px;">
    <p style="margin:0;font-size:14px;color:#374151;">You can now send each other birthday, festival and everyday wishes, and chat together.</p>
  </div>
  %s
  <p style="margin:0;font-size:13px;color:%s;">You are receiving this because you sent a circle request to %s.</p>
</div>
"""
        .formatted(
            FONT,
            TEXT,
            escape(requester.getName()),
            escape(addresseeName),
            ctaButton(link, "Open Golden Greetings"),
            MUTED,
            escape(addresseeName));
  }

  /** A bulletproof, table-based call-to-action button with a copyable fallback link. */
  private String ctaButton(String url, String label) {
    String safeUrl = escape(url);
    return "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" align=\"center\""
        + " style=\"margin:8px auto 12px;\"><tr><td align=\"center\" bgcolor=\""
        + ACCENT
        + "\" style=\"border-radius:8px;\"><a href=\""
        + safeUrl
        + "\" target=\"_blank\" rel=\"noopener noreferrer\" style=\"display:inline-block;padding:12px 28px;"
        + FONT
        + "font-size:16px;font-weight:bold;color:#ffffff;text-decoration:none;border-radius:8px;\">"
        + escape(label)
        + "</a></td></tr></table>"
        + "<p style=\"text-align:center;margin:0 0 16px;"
        + FONT
        + "font-size:12px;color:"
        + MUTED
        + ";\">Button not working? Copy this link into your browser:<br/>"
        + "<a href=\""
        + safeUrl
        + "\" target=\"_blank\" rel=\"noopener noreferrer\" style=\"color:"
        + ACCENT
        + ";word-break:break-all;\">"
        + safeUrl
        + "</a></p>";
  }

  private String escape(String value) {
    return WishEmailExtras.escape(value);
  }
}
