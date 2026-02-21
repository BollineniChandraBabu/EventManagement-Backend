package com.familywishes.service;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.EmailDtos;

public interface GmailEmailService {
  void sendEmailWithAttachments(String to, String subject, String html, Long logId, byte[] image);

  void retryFailed();

  PagedResponse<EmailDtos.EmailStatusResponse> getStatus(
      int page, int size, String searchKey, String mailTab, String requesterEmail, boolean isAdmin);

  void sendTestEmail(String to);
}
