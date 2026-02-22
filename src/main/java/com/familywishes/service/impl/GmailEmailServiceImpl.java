package com.familywishes.service.impl;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.EmailDtos;
import com.familywishes.entity.EmailLog;
import com.familywishes.entity.EventTypeSeed;
import com.familywishes.entity.enums.EmailStatus;
import com.familywishes.entity.enums.EmailType;
import com.familywishes.exception.NotFoundException;
import com.familywishes.repository.EmailLogRepository;
import com.familywishes.repository.EventTypeSeedRepository;
import com.familywishes.repository.UserRepository;
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
  private final UserRepository userRepository;
  private final EventTypeSeedRepository eventTypeSeedRepository;

  private final Gmail gmail;

  @Value("${gmail.from-email}")
  private String senderEmail;

  private static final List<EmailType> SENSITIVE_TYPES =
      List.of(EmailType.OTP, EmailType.FORGOT_PASSWORD);

  private static final List<EmailType> NON_SENSITIVE_TYPES =
      List.of(EmailType.GOOD_MORNING, EmailType.GOOD_NIGHT, EmailType.BIRTHDAY, EmailType.EVENT);

  // ==========================
  // SEND EMAIL
  // ==========================

  @Override
  public void sendEmailWithAttachments(
      String to, String subject, String html, Long logId, byte[] image) {

    EmailLog logEntry =
        logId == null
            ? createPendingEmailLog(to, subject, classifyEmailType(subject, html))
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
    List<EmailType> filteredTypes = resolveNonSensitiveTypesFromTab(mailTab);

    Page<EmailLog> logs =
        isAdmin
            ? logRepository.findAllBySearchKeyAndEmailTypeIn(
                normalizedSearchKey, filteredTypes, pageRequest)
            : logRepository.findAllByRecipientEmailAndSearchKeyAndEmailTypeIn(
                requesterEmail, normalizedSearchKey, filteredTypes, pageRequest);

    return new PagedResponse<>(
        logs.getContent().stream().map(this::toEmailStatusResponse).toList(),
        logs.getNumber(),
        logs.getSize(),
        logs.getTotalElements(),
        logs.getTotalPages(),
        logs.hasNext(),
        logs.hasPrevious());
  }


  @Override
  public PagedResponse<EmailDtos.EmailStatusResponse> getOtpStatus(
      int page, int size, String searchKey, String sortBy, String sortDir) {
    return getAdminStatusByTypes(page, size, searchKey, sortBy, sortDir, List.of(EmailType.OTP));
  }

  @Override
  public PagedResponse<EmailDtos.EmailStatusResponse> getForgotPasswordStatus(
      int page, int size, String searchKey, String sortBy, String sortDir) {
    return getAdminStatusByTypes(
        page, size, searchKey, sortBy, sortDir, List.of(EmailType.FORGOT_PASSWORD));
  }

  @Override
  public EmailDtos.EmailStatusResponse getStatusById(Long id, String requesterEmail) {
    EmailLog log =
        logRepository.findById(id).orElseThrow(() -> new NotFoundException("Email log not found"));

    if (!log.getRecipientEmail().equalsIgnoreCase(requesterEmail)) {
      throw new NotFoundException("Email log not found");
    }
    if (SENSITIVE_TYPES.contains(log.getEmailType())) {
      throw new NotFoundException("Email log not found");
    }

    return toEmailStatusResponse(log);
  }

  @Override
  public void sendTestEmail(String to) {
    sendEmailWithAttachments(to, "Test Email", "<h3>Family Wishes Gmail API test</h3>", null, null);
  }

  @Override
  public void sendEmailNow(EmailDtos.SendEmailNowRequest request) {
    userRepository
        .findByEmailAndDeletedFalse(request.userEmail())
        .orElseThrow(() -> new NotFoundException("User not found"));

    EventTypeSeed eventTypeSeed =
        eventTypeSeedRepository
            .findByCodeAndActiveTrue(request.eventTypeCode().trim().toUpperCase(Locale.ROOT))
            .orElseThrow(() -> new NotFoundException("Event type not found"));

    String htmlBody =
        "<p><strong>Event Type:</strong> "
            + eventTypeSeed.getDisplayName()
            + " ("
            + eventTypeSeed.getCode()
            + ")</p><br/>"
            + request.body();

    EmailLog logEntry = createPendingEmailLog(request.userEmail(), request.subject(), EmailType.EVENT);
    sendEmailWithAttachments(
        request.userEmail(), request.subject(), htmlBody, logEntry.getId(), null);
  }

  private EmailDtos.EmailStatusResponse toEmailStatusResponse(EmailLog log) {
    return new EmailDtos.EmailStatusResponse(
        log.getId(),
        log.getRecipientEmail(),
        log.getSubject(),
        log.getBody(),
        log.getImageData(),
        log.getStatus().name(),
        log.getEmailType() == null ? EmailType.EVENT.name() : log.getEmailType().name(),
        log.getSentAt());
  }

  private PagedResponse<EmailDtos.EmailStatusResponse> getAdminStatusByTypes(
      int page, int size, String searchKey, String sortBy, String sortDir, List<EmailType> types) {
    String normalizedSearchKey = searchKey == null ? "" : searchKey.trim();
    Sort.Direction direction =
        "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
    String normalizedSortBy = (sortBy == null || sortBy.isBlank()) ? "id" : sortBy.trim();
    PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, normalizedSortBy));

    Page<EmailLog> logs =
        logRepository.findAllBySearchKeyAndEmailTypeIn(normalizedSearchKey, types, pageRequest);

    return new PagedResponse<>(
        logs.getContent().stream().map(this::toEmailStatusResponse).toList(),
        logs.getNumber(),
        logs.getSize(),
        logs.getTotalElements(),
        logs.getTotalPages(),
        logs.hasNext(),
        logs.hasPrevious());
  }

  private List<EmailType> resolveNonSensitiveTypesFromTab(String mailTab) {
    String normalizedTab = mailTab == null ? "ALL" : mailTab.trim().toUpperCase(Locale.ROOT);

    return switch (normalizedTab) {
      case "GOOD_MORNING" -> List.of(EmailType.GOOD_MORNING);
      case "GOOD_NIGHT" -> List.of(EmailType.GOOD_NIGHT);
      case "BIRTHDAY" -> List.of(EmailType.BIRTHDAY);
      case "EVENT" -> List.of(EmailType.EVENT);
      default -> NON_SENSITIVE_TYPES;
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

  private EmailLog createPendingEmailLog(String to, String subject, EmailType emailType) {
    return logRepository.save(
        EmailLog.builder()
            .recipientEmail(to)
            .subject(subject)
            .status(EmailStatus.PENDING)
            .emailType(emailType)
            .retryCount(0)
            .build());
  }
}
