package com.familywishes.service.impl;

import com.familywishes.entity.MessageLog;
import com.familywishes.entity.MessageStatus;
import com.familywishes.repository.IGMessageLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstagramService {

    private final RestTemplate restTemplate;
    private final IGMessageLogRepository messageLogRepository;

    @Value("${instagram.graph-url}")
    private String graphUrl;

    @Value("${instagram.page-access-token}")
    private String pageAccessToken;

    /**
     * Sends message to Instagram user (Stub implementation)
     */
    @Async
    public void sendMessage(MessageLog messageLog) {

        messageLog.setLastAttemptTime(LocalDateTime.now());
        messageLog.setStatus(MessageStatus.PENDING);
        messageLogRepository.save(messageLog);

        try {
            String url = graphUrl + "/me/messages?access_token=" + pageAccessToken;

            String requestBody = buildRequestBody(messageLog.getInstagramUserId(), messageLog.getMessage());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request =
                    new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                messageLog.setStatus(MessageStatus.SENT);
                log.info("Message sent successfully to {}", messageLog.getInstagramUserId());
            } else {
                messageLog.setStatus(MessageStatus.FAILED);
                log.error("Failed to send message: {}", response.getBody());
            }

        } catch (Exception ex) {
            messageLog.setStatus(MessageStatus.FAILED);
            log.error("Error sending Instagram message", ex);
        }

        messageLogRepository.save(messageLog);
    }

    /**
     * Stub payload builder (Graph API compatible)
     */
    private String buildRequestBody(String recipientId, String messageText) {
        return """
                {
                  "recipient": {
                    "id": "%s"
                  },
                  "message": {
                    "text": "%s"
                  }
                }
                """.formatted(recipientId, messageText);
    }
}