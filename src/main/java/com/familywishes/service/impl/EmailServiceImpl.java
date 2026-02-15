package com.familywishes.service.impl;

import com.familywishes.dto.EmailDtos.EmailStatusResponse;
import com.familywishes.entity.EmailLog;
import com.familywishes.entity.enums.EmailStatus;
import com.familywishes.repository.EmailLogRepository;
import com.familywishes.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender mailSender;
    private final EmailLogRepository logRepository;
    private final JavaMailSender sender;
    private final Environment env;

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
            logEntry.setBody(html);
            logEntry.setStatus(EmailStatus.SENT);
            logEntry.setSentAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("mail send failed", e);
            logEntry.setStatus(EmailStatus.FAILED);
            logEntry.setBody(html);
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

    @Override
    public List<EmailStatusResponse> getStatus() {
        List<EmailStatusResponse> emailStatusResponses = new ArrayList<>();
       List<EmailLog> emailLogList = logRepository.findAll();

//        long pending = logRepository.countByStatus(EmailStatus.PENDING);
//        long sent = logRepository.countByStatus(EmailStatus.SENT);
//        long failed = logRepository.countByStatus(EmailStatus.FAILED);
        emailLogList.forEach(emailLog -> {
            EmailStatusResponse emailStatusResponse = new EmailStatusResponse(emailLog.getId(), emailLog.getRecipientEmail(), emailLog.getSubject(), emailLog.getStatus().name(), emailLog.getSentAt());
            emailStatusResponses.add(emailStatusResponse);
        });
        return emailStatusResponses;
    }

    public void sendFailureAlert(long failedCount) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(env.getProperty("alert.email.to"));
        msg.setSubject("⚠ Instagram Message Failure Alert");

        msg.setText(
                "High failure detected.\n\n" +
                        "Today's failed messages: " + failedCount +
                        "\nPlease check dashboard immediately."
        );
        sender.send(msg);
    }
}
