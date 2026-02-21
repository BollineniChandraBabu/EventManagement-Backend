package com.familywishes.service.impl;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.EmailDtos;
import com.familywishes.entity.EmailLog;
import com.familywishes.entity.enums.EmailStatus;
import com.familywishes.entity.enums.EmailType;
import com.familywishes.repository.EmailLogRepository;
import com.familywishes.service.GmailEmailService;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GmailEmailServiceImpl implements GmailEmailService {
  private final EmailLogRepository logRepository;

  private final Gmail gmail;

  @Value("${gmail.from-email}")
  private String senderEmail;

  // ==========================
  // SEND EMAIL
  // ==========================

  @Override
  public void sendEmailWithAttachments(
      String to, String subject, String html, Long logId, byte[] image) {

    EmailLog logEntry =
        logId == null
            ? logRepository.save(
                EmailLog.builder()
                    .recipientEmail(to)
                    .subject(subject)
                    .status(EmailStatus.PENDING)
                    .emailType(classifyEmailType(subject, html))
                    .retryCount(0)
                    .build())
            : logRepository.findById(logId).orElseThrow();

    if (logEntry.getEmailType() == null) {
      logEntry.setEmailType(classifyEmailType(subject, html));
    }

    try {

      MimeMessage mimeMessage = createMimeMessage(to, subject, html, image);

      ByteArrayOutputStream buffer = new ByteArrayOutputStream();

      mimeMessage.writeTo(buffer);

      String encodedEmail = Base64.getUrlEncoder().encodeToString(buffer.toByteArray());

      Message message = new Message();

      message.setRaw(encodedEmail);

      gmail.users().messages().send("me", message).execute();

      logEntry.setBody(html);
      logEntry.setImageData(image);
      logEntry.setStatus(EmailStatus.SENT);

      logEntry.setSentAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));

      log.info("Email sent successfully to {}", to);

    } catch (Exception e) {

      log.error("mail send failed", e);

      logEntry.setStatus(EmailStatus.FAILED);

      logEntry.setBody(html);
      logEntry.setImageData(image);
      logEntry.setRetryCount(logEntry.getRetryCount() + 1);

      logEntry.setErrorMessage(e.getMessage());
    }

    logRepository.save(logEntry);
  }

  // ==========================
  // CREATE MIME MESSAGE
  // ==========================

  private MimeMessage createMimeMessage(String to, String subject, String html, byte[] image)
      throws Exception {

    Session session = Session.getInstance(new Properties(), null);

    MimeMessage message = new MimeMessage(session);

    message.setFrom(new InternetAddress(senderEmail));

    message.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress(to));

    message.setSubject(subject);

    MimeMultipart multipart = new MimeMultipart("related");

    MimeBodyPart htmlPart = new MimeBodyPart();

    htmlPart.setContent(html, "text/html");

    multipart.addBodyPart(htmlPart);

    // INLINE IMAGE SUPPORT

    if (image != null) {

      MimeBodyPart imagePart = new MimeBodyPart();

      imagePart.setDataHandler(
          new jakarta.activation.DataHandler(new ByteArrayDataSource(image, "image/png")));

      imagePart.setHeader("Content-ID", "<birthdayImage>");

      multipart.addBodyPart(imagePart);
    }

    message.setContent(multipart);

    return message;
  }

  // ==========================
  // RETRY
  // ==========================

  @Override
  public void retryFailed() {

    logRepository
        .findByStatusAndRetryCountLessThan(EmailStatus.FAILED, 3)
        .forEach(
            log ->
                sendEmailWithAttachments(
                    log.getRecipientEmail(),
                    log.getSubject(),
                    log.getBody(),
                    log.getId(),
                    log.getImageData()));
  }

  // ==========================
  // STATUS
  // ==========================

  @Override
  public PagedResponse<EmailDtos.EmailStatusResponse> getStatus(
      int page,
      int size,
      String searchKey,
      String mailTab,
      String requesterEmail,
      boolean isAdmin,
      String sortBy,
      String sortDir) {
    String normalizedSearchKey = searchKey == null ? "" : searchKey.trim();
    Sort.Direction direction =
        "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
    String normalizedSortBy = (sortBy == null || sortBy.isBlank()) ? "id" : sortBy.trim();
    PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, normalizedSortBy));
    List<EmailType> filteredTypes = resolveTypesFromTab(mailTab);

    Page<EmailLog> logs;
    if (isAdmin) {
      logs =
          filteredTypes == null
              ? logRepository.findAllBySearchKey(normalizedSearchKey, pageRequest)
              : logRepository.findAllBySearchKeyAndEmailTypeIn(
                  normalizedSearchKey, filteredTypes, pageRequest);
    } else {
      logs =
          filteredTypes == null
              ? logRepository.findAllByRecipientEmailAndSearchKey(
                  requesterEmail, normalizedSearchKey, pageRequest)
              : logRepository.findAllByRecipientEmailAndSearchKeyAndEmailTypeIn(
                  requesterEmail, normalizedSearchKey, filteredTypes, pageRequest);
    }

    return new PagedResponse<>(
        logs.getContent().stream()
            .map(
                log ->
                    new EmailDtos.EmailStatusResponse(
                        log.getId(),
                        log.getRecipientEmail(),
                        log.getSubject(),
                        log.getBody(),
                        log.getImageData(),
                        log.getStatus().name(),
                        log.getEmailType() == null
                            ? EmailType.EVENT.name()
                            : log.getEmailType().name(),
                        log.getSentAt()))
            .toList(),
        logs.getNumber(),
        logs.getSize(),
        logs.getTotalElements(),
        logs.getTotalPages(),
        logs.hasNext(),
        logs.hasPrevious());
  }

  @Override
  public void sendTestEmail(String to) {
    sendEmailWithAttachments(to, "Test Email", "<h3>Family Wishes Gmail API test</h3>", null, null);
  }

  private List<EmailType> resolveTypesFromTab(String mailTab) {
    String normalizedTab = mailTab == null ? "ALL" : mailTab.trim().toUpperCase(Locale.ROOT);

    return switch (normalizedTab) {
      case "OTP" -> List.of(EmailType.OTP);
      case "FORGOT_PASSWORD", "FORGOT" -> List.of(EmailType.FORGOT_PASSWORD);
      case "WISHES_EVENTS", "OTHERS", "GOOD_MORNING_GOOD_NIGHT_EVENTS" ->
          List.of(
              EmailType.GOOD_MORNING, EmailType.GOOD_NIGHT, EmailType.BIRTHDAY, EmailType.EVENT);
      default -> null;
    };
  }

  private EmailType classifyEmailType(String subject, String html) {
    String content =
        ((subject == null ? "" : subject) + " " + (html == null ? "" : html)).toLowerCase();

    if (content.contains("otp")) {
      return EmailType.OTP;
    }
    if (content.contains("reset your")
        || content.contains("forgot password")
        || content.contains("password reset")) {
      return EmailType.FORGOT_PASSWORD;
    }
    if (content.contains("good morning")) {
      return EmailType.GOOD_MORNING;
    }
    if (content.contains("good night")) {
      return EmailType.GOOD_NIGHT;
    }
    if (content.contains("birthday")) {
      return EmailType.BIRTHDAY;
    }

    return EmailType.EVENT;
  }
}
