package com.familywishes.service;

import com.familywishes.dto.EmailDtos;

import java.util.List;

public interface GmailEmailService {
    void sendEmailWithAttachments(String to, String subject, String html, Long logId, byte[] image);
    void retryFailed();
    List<EmailDtos.EmailStatusResponse> getStatus();
    void sendTestEmail(String to);
}
