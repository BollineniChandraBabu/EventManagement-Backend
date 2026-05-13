package com.familywishes.service.impl;

import com.familywishes.entity.EmailLog;
import com.familywishes.entity.enums.EmailStatus;
import com.familywishes.repository.EmailLogRepository;
import com.familywishes.repository.UserRepository;
import com.familywishes.service.EmailService;
import com.familywishes.util.EmailTemplateBuilder;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {
  private final JavaMailSender mailSender;
  private final EmailLogRepository logRepository;
  private final JavaMailSender sender;
  private final UserRepository userRepository;
  private final Environment env;
  private final SupabaseStorageService supabaseStorageService;
  private final EmailTemplateBuilder emailTemplateBuilder;

  @Value("${scheduler.time-zone:Asia/Kolkata}")
  private String schedulerTimeZone;

  @Override
  public void sendHtmlEmail(String to, String subject, String html, Long logId, byte[] image) {
    EmailLog logEntry =
        logId == null
            ? logRepository.save(
                EmailLog.builder()
                    .recipientUser(resolveRecipientUser(to))
                    .subject(subject)
                    .status(EmailStatus.PENDING)
                    .retryCount(0)
                    .build())
            : logRepository.findById(logId).orElseThrow();
    try {
      byte[] signatureImage = supabaseStorageService.getEmailSignatureImage();
      String signatureSource =
          signatureImage != null && signatureImage.length > 0
              ? "cid:emailSignature"
              : supabaseStorageService.getEmailSignatureUrl();
      String finalHtml = emailTemplateBuilder.build(html, signatureSource);
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true);
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(finalHtml, true);
      if (Objects.nonNull(image)) {
        helper.addInline("birthdayImage", new ByteArrayResource(image), "image/png");
      }
      if (Objects.nonNull(signatureImage) && signatureImage.length > 0) {
        helper.addInline("emailSignature", new ByteArrayResource(signatureImage), "image/png");
      }
      mailSender.send(message);
      logEntry.setBody(html);
      logEntry.setStatus(EmailStatus.SENT);
      logEntry.setSentAt(LocalDateTime.now(ZoneId.of(schedulerTimeZone)));
    } catch (Exception e) {
      log.error("mail send failed", e);
      logEntry.setStatus(EmailStatus.FAILED);
      logEntry.setBody(html);
      logEntry.setRetryCount(logEntry.getRetryCount() + 1);
      logEntry.setErrorMessage(e.getMessage());
      logEntry.setSentAt(LocalDateTime.now(ZoneId.of(schedulerTimeZone)));
    }
    logRepository.save(logEntry);
  }

  private com.familywishes.entity.User resolveRecipientUser(String to) {
    if (to == null || to.isBlank()) {
      return null;
    }
    return userRepository.findByEmailAndDeletedFalse(to).orElse(null);
  }

  @Override
  public void retryFailed() {
    logRepository
        .findByStatusAndRetryCountLessThan(EmailStatus.FAILED, 3)
        .forEach(
            log ->
                sendHtmlEmail(
                    log.getRecipientEmail(), log.getSubject(), log.getBody(), log.getId(), null));
  }

  public void sendFailureAlert(long failedCount) {
    SimpleMailMessage msg = new SimpleMailMessage();
    msg.setTo(env.getProperty("alert.email.to"));
    msg.setSubject("⚠ Instagram Message Failure Alert");

    msg.setText(
        "High failure detected.\n\n"
            + "Today's failed messages: "
            + failedCount
            + "\nPlease check dashboard immediately.");
    sender.send(msg);
  }
}
