package com.familywishes.service.impl;

import com.familywishes.entity.EmailLog;
import com.familywishes.entity.enums.EmailStatus;
import com.familywishes.repository.EmailLogRepository;
import com.familywishes.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender mailSender;
    private final EmailLogRepository logRepository;

    @Override
    public void sendHtmlEmail(String to, String subject, String html, Long logId) {
        EmailLog logEntry = logId == null ? logRepository.save(EmailLog.builder().recipientEmail(to).subject(subject).status(EmailStatus.PENDING).retryCount(0).build())
                : logRepository.findById(logId).orElseThrow();
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            logEntry.setStatus(EmailStatus.SENT);
            logEntry.setSentAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("mail send failed", e);
            logEntry.setStatus(EmailStatus.FAILED);
            logEntry.setRetryCount(logEntry.getRetryCount() + 1);
            logEntry.setErrorMessage(e.getMessage());
        }
        logRepository.save(logEntry);
    }

    @Override
    public void sendTestEmail(String to) {
        sendHtmlEmail(to, "Test Email", "<h3>Family Wishes test email</h3>", null);
    }

    @Override
    public void retryFailed() {
        logRepository.findByStatusAndRetryCountLessThan(EmailStatus.FAILED, 3)
                .forEach(log -> sendHtmlEmail(log.getRecipientEmail(), log.getSubject(), "Retry delivery", log.getId()));
    }
}
