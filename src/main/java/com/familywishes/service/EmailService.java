package com.familywishes.service;

import com.familywishes.dto.EmailDtos.EmailStatusResponse;

import java.util.List;

public interface EmailService {
    void sendHtmlEmail(String to, String subject, String html, Long logId);
    void sendTestEmail(String to);
    void retryFailed();
    List<EmailStatusResponse> getStatus();
    void sendFailureAlert(long count);
}
