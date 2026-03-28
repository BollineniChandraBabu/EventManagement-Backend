package com.familywishes.service.impl;

import com.familywishes.dto.EmailDtos;
import com.familywishes.entity.EmailLog;
import com.familywishes.entity.enums.EmailStatus;
import com.familywishes.entity.enums.EmailType;
import com.familywishes.repository.EmailLogRepository;
import com.familywishes.service.BrevoEmailService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class BrevoEmailServiceImpl implements BrevoEmailService {
  private final EmailLogRepository logRepository;
  private final RestTemplate restTemplate;
  private final SupabaseStorageService supabaseStorageService;

  @Value("${brevo.api.key}")
  private String brevoApiKey;

  @Value("${brevo.sender.email}")
  private String senderEmail;

  @Value("${brevo.sender.name}")
  private String senderName;

  @Value("${brevo.api.url}")
  private String brevoUrl;

  public void sendEmail(String to, String subject, String htmlContent) {

    sendEmailWithAttachments(to, subject, htmlContent, null, null);
  }

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
      HttpHeaders headers = new HttpHeaders();
      headers.set("api-key", brevoApiKey);
      headers.setContentType(MediaType.APPLICATION_JSON);
      Map<String, Object> body = new HashMap<>();
      // Sender
      body.put(
          "sender",
          Map.of(
              "email", senderEmail,
              "name", senderName));
      // Recipient
      body.put("to", List.of(Map.of("email", to)));
      body.put("subject", subject);
      // ========================
      // INLINE IMAGE SUPPORT
      // ========================
      if (image != null) {
        String base64 = Base64.getEncoder().encodeToString(image);
        body.put("htmlContent", html);
        body.put("inlineImage", Map.of("birthdayImage", base64));
      } else {
        body.put("htmlContent", html);
      }
      HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
      ResponseEntity<String> response = restTemplate.postForEntity(brevoUrl, request, String.class);
      // SUCCESS
      logEntry.setBody(html);
      if (image != null) {
        logEntry.setImageUrl(supabaseStorageService.uploadEmailImage(image, logEntry.getEmailType()));
      }
      logEntry.setStatus(EmailStatus.SENT);
      logEntry.setSentAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
      log.info("Email sent: {}", response.getBody());
    } catch (Exception e) {
      log.error("mail send failed", e);
      logEntry.setStatus(EmailStatus.FAILED);
      logEntry.setBody(html);
      if (image != null) {
        logEntry.setImageUrl(supabaseStorageService.uploadEmailImage(image, logEntry.getEmailType()));
      }
      logEntry.setRetryCount(logEntry.getRetryCount() + 1);
      logEntry.setErrorMessage(e.getMessage());
    }
    logRepository.save(logEntry);
  }

  @Override
  public void retryFailed() {
    logRepository
        .findByStatusAndRetryCountLessThan(EmailStatus.FAILED, 3)
        .forEach(
            log ->
                sendEmailWithAttachments(
                    log.getRecipientEmail(),
                    log.getSubject(),
                    "Retry delivery",
                    log.getId(),
                    null));
  }

  @Override
  public List<EmailDtos.EmailStatusResponse> getStatus() {
    List<EmailDtos.EmailStatusResponse> emailStatusResponses = new ArrayList<>();
    List<EmailLog> emailLogList = logRepository.findAll();
    emailLogList.forEach(
        emailLog -> {
          EmailDtos.EmailStatusResponse emailStatusResponse =
              new EmailDtos.EmailStatusResponse(
                  emailLog.getId(),
                  emailLog.getRecipientEmail(),
                  emailLog.getSubject(),
                  emailLog.getBody(),
                  supabaseStorageService.downloadImage(emailLog.getImageUrl()),
                  emailLog.getStatus().name(),
                  emailLog.getEmailType() == null
                      ? EmailType.EVENT.name()
                      : emailLog.getEmailType().name(),
                  emailLog.getSentAt());
          emailStatusResponses.add(emailStatusResponse);
        });
    return emailStatusResponses;
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

  @Override
  public void sendTestEmail(String to) {
    sendEmailWithAttachments(to, "Test Email", "<h3>Family Wishes test email</h3>", null, null);
  }
}
