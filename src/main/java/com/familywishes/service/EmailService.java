package com.familywishes.service;

public interface EmailService {
    void sendHtmlEmail(String to, String subject, String html, Long logId);
    void sendTestEmail(String to);
    void retryFailed();
}
