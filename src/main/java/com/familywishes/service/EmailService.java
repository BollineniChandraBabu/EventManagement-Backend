package com.familywishes.service;

public interface EmailService {
  void sendHtmlEmail(String to, String subject, String html, Long logId, byte[] image);

  void retryFailed();

  void sendFailureAlert(long count);
}
