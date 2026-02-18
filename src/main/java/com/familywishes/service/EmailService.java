package com.familywishes.service;

import com.familywishes.dto.EmailDtos.EmailStatusResponse;

import java.util.List;

public interface EmailService {
    void sendHtmlEmail(String to, String subject, String html, Long logId, byte[] image);
    void retryFailed();
    void sendFailureAlert(long count);
}
