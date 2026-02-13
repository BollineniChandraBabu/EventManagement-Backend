package com.familywishes.service;

import com.familywishes.dto.EmailDtos.EmailStatusResponse;

public interface EmailService {
    void sendHtmlEmail(String to, String subject, String html, Long logId);
    void sendTestEmail(String to);
    void retryFailed();
    EmailStatusResponse getStatus();
}
