package com.familywishes.service;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.EmailDtos;
import com.familywishes.dto.EmailImage;

public interface GmailEmailService {
  void sendEmailWithAttachments(String to, String subject, String html, Long logId, byte[] image);

  void retryFailed();

  PagedResponse<EmailDtos.EmailStatusResponse> getStatus(
      int page,
      int size,
      String searchKey,
      String mailTab,
      String requesterEmail,
      boolean isAdmin,
      String sortBy,
      String sortDir);

  PagedResponse<EmailDtos.EmailStatusResponse> getStatusWithFilters(
      int page,
      int size,
      String searchKey,
      java.util.List<com.familywishes.entity.enums.EmailStatus> statuses,
      java.util.List<com.familywishes.entity.enums.EmailType> emailTypes,
      String requesterEmail,
      boolean isAdmin,
      String sortBy,
      String sortDir);

  PagedResponse<EmailDtos.EmailStatusResponse> getOtpStatus(
      int page, int size, String searchKey, String sortBy, String sortDir);

  PagedResponse<EmailDtos.EmailStatusResponse> getForgotPasswordStatus(
      int page, int size, String searchKey, String sortBy, String sortDir);

  PagedResponse<EmailDtos.EmailStatusResponse> getFestivalWishStatus(
      int page,
      int size,
      String searchKey,
      String requesterEmail,
      boolean isAdmin,
      String sortBy,
      String sortDir);

  PagedResponse<EmailDtos.EmailStatusResponse> getUnreadChatMessageStatus(
      int page,
      int size,
      String searchKey,
      String requesterEmail,
      boolean isAdmin,
      String sortBy,
      String sortDir);

  EmailDtos.EmailStatusResponse getStatusById(Long id, String requesterEmail);

  /**
   * The image of one email, loaded on demand (the status lists only say whether there is one).
   * Admins may open any email; everyone else only their own non-sensitive ones.
   */
  EmailImage getImage(Long id, String requesterEmail, boolean isAdmin);

  void sendTestEmail(String to);

  void sendEmailNow(EmailDtos.SendEmailNowRequest request);

  void sendEmailWithAttachments(
      String to,
      String subject,
      String html,
      Long logId,
      byte[] image,
      com.familywishes.entity.enums.EmailType emailType);

  void deleteEmailStatus(Long id);
}
