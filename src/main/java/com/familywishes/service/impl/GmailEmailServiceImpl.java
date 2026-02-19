package com.familywishes.service.impl;

import com.familywishes.dto.EmailDtos;
import com.familywishes.entity.EmailLog;
import com.familywishes.entity.enums.EmailStatus;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

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
            String to,
            String subject,
            String html,
            Long logId,
            byte[] image
    ) {

        EmailLog logEntry = logId == null
                ? logRepository.save(
                EmailLog.builder()
                        .recipientEmail(to)
                        .subject(subject)
                        .status(EmailStatus.PENDING)
                        .retryCount(0)
                        .build()
        )
                : logRepository.findById(logId).orElseThrow();

        try {

            MimeMessage mimeMessage =
                    createMimeMessage(to, subject, html, image);

            ByteArrayOutputStream buffer =
                    new ByteArrayOutputStream();

            mimeMessage.writeTo(buffer);

            String encodedEmail =
                    Base64.getUrlEncoder()
                            .encodeToString(buffer.toByteArray());

            Message message = new Message();

            message.setRaw(encodedEmail);

            gmail.users()
                    .messages()
                    .send("me", message)
                    .execute();

            logEntry.setBody(html);

            logEntry.setStatus(EmailStatus.SENT);

            logEntry.setSentAt(LocalDateTime.now());

            log.info("Email sent successfully to {}", to);


        } catch (Exception e) {

            log.error("mail send failed", e);

            logEntry.setStatus(EmailStatus.FAILED);

            logEntry.setBody(html);

            logEntry.setRetryCount(
                    logEntry.getRetryCount() + 1
            );

            logEntry.setErrorMessage(e.getMessage());
        }

        logRepository.save(logEntry);
    }


    // ==========================
    // CREATE MIME MESSAGE
    // ==========================

    private MimeMessage createMimeMessage(
            String to,
            String subject,
            String html,
            byte[] image
    ) throws Exception {

        Session session =
                Session.getInstance(new Properties(), null);

        MimeMessage message =
                new MimeMessage(session);

        message.setFrom(
                new InternetAddress(senderEmail)
        );

        message.addRecipient(
                MimeMessage.RecipientType.TO,
                new InternetAddress(to)
        );

        message.setSubject(subject);


        MimeMultipart multipart =
                new MimeMultipart("related");


        MimeBodyPart htmlPart =
                new MimeBodyPart();

        htmlPart.setContent(html, "text/html");

        multipart.addBodyPart(htmlPart);


        // INLINE IMAGE SUPPORT

        if (image != null) {

            MimeBodyPart imagePart =
                    new MimeBodyPart();

            imagePart.setDataHandler(
                    new jakarta.activation.DataHandler(
                            new ByteArrayDataSource(
                                    image,
                                    "image/png"
                            )
                    )
            );

            imagePart.setHeader(
                    "Content-ID",
                    "<birthdayImage>"
            );

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
                .findByStatusAndRetryCountLessThan(
                        EmailStatus.FAILED,
                        3
                )
                .forEach(log ->
                        sendEmailWithAttachments(
                                log.getRecipientEmail(),
                                log.getSubject(),
                                log.getBody(),
                                log.getId(),
                                null
                        ));
    }


    // ==========================
    // STATUS
    // ==========================

    @Override
    public List<EmailDtos.EmailStatusResponse> getStatus() {

        List<EmailDtos.EmailStatusResponse> list =
                new ArrayList<>();

        logRepository.findAll()
                .forEach(log ->

                        list.add(

                                new EmailDtos.EmailStatusResponse(

                                        log.getId(),

                                        log.getRecipientEmail(),

                                        log.getSubject(),

                                        log.getStatus().name(),

                                        log.getSentAt()

                                )

                        ));

        return list;
    }


    // ==========================
    // TEST EMAIL
    // ==========================

    @Override
    public void sendTestEmail(String to) {

        sendEmailWithAttachments(

                to,

                "Test Email",

                "<h3>Family Wishes Gmail API test</h3>",

                null,

                null

        );
    }
}
