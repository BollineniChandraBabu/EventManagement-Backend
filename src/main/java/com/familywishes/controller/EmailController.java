package com.familywishes.controller;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.EmailDtos.EmailStatusResponse;
import com.familywishes.dto.EmailDtos.SendEmailNowRequest;
import com.familywishes.service.GmailEmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/email", "/api/emails"})
@RequiredArgsConstructor
public class EmailController {
  private final GmailEmailService emailService;

  @PostMapping("/test")
  public void test(Authentication authentication) {
    emailService.sendTestEmail(authentication.getName());
  }

  @PostMapping("/send-now")
  @PreAuthorize("hasRole('ADMIN')")
  public void sendNow(@Valid @RequestBody SendEmailNowRequest request) {
    emailService.sendEmailNow(request);
  }

  @GetMapping("/status/{id}")
  public EmailStatusResponse statusById(@PathVariable Long id, Authentication authentication) {
    return emailService.getStatusById(id, authentication.getName());
  }

  @GetMapping("/status")
  public PagedResponse<EmailStatusResponse> status(
      Authentication authentication,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "") String searchKey,
      @RequestParam(defaultValue = "ALL") String mailTab,
      @RequestParam(defaultValue = "id") String sortBy,
      @RequestParam(defaultValue = "desc") String sortDir) {
    boolean isAdmin =
        authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch("ROLE_ADMIN"::equals);

    return emailService.getStatus(
        page, size, searchKey, mailTab, authentication.getName(), isAdmin, sortBy, sortDir);
  }

  @GetMapping("/status/admin/otp")
  @PreAuthorize("hasRole('ADMIN')")
  public PagedResponse<EmailStatusResponse> otpStatus(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "") String searchKey,
      @RequestParam(defaultValue = "id") String sortBy,
      @RequestParam(defaultValue = "desc") String sortDir) {
    return emailService.getOtpStatus(page, size, searchKey, sortBy, sortDir);
  }

  @GetMapping("/status/admin/forgot-password")
  @PreAuthorize("hasRole('ADMIN')")
  public PagedResponse<EmailStatusResponse> forgotPasswordStatus(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "") String searchKey,
      @RequestParam(defaultValue = "id") String sortBy,
      @RequestParam(defaultValue = "desc") String sortDir) {
    return emailService.getForgotPasswordStatus(page, size, searchKey, sortBy, sortDir);
  }

  @GetMapping("/status/festival-wishes")
  public PagedResponse<EmailStatusResponse> festivalWishStatus(
      Authentication authentication,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "") String searchKey,
      @RequestParam(defaultValue = "id") String sortBy,
      @RequestParam(defaultValue = "desc") String sortDir) {
    boolean isAdmin =
        authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch("ROLE_ADMIN"::equals);
    return emailService.getFestivalWishStatus(
        page, size, searchKey, authentication.getName(), isAdmin, sortBy, sortDir);
  }

  @GetMapping("/status/unread-chat-messages")
  public PagedResponse<EmailStatusResponse> unreadChatMessageStatus(
      Authentication authentication,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "") String searchKey,
      @RequestParam(defaultValue = "id") String sortBy,
      @RequestParam(defaultValue = "desc") String sortDir) {
    boolean isAdmin =
        authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch("ROLE_ADMIN"::equals);
    return emailService.getUnreadChatMessageStatus(
        page, size, searchKey, authentication.getName(), isAdmin, sortBy, sortDir);
  }
}
