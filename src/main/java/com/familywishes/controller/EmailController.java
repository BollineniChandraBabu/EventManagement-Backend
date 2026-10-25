package com.familywishes.controller;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.EmailDtos.EmailStatusResponse;
import com.familywishes.dto.EmailImage;
import com.familywishes.dto.EmailDtos.SendEmailNowRequest;
import com.familywishes.service.GmailEmailService;
import jakarta.validation.Valid;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

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

  /** Loaded separately from the list so a page of emails never waits for image downloads. */
  @GetMapping("/status/{id}/image")
  public ResponseEntity<byte[]> statusImage(@PathVariable Long id, Authentication authentication) {
    boolean isAdmin =
        authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch("ROLE_ADMIN"::equals);
    EmailImage image = emailService.getImage(id, authentication.getName(), isAdmin);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(image.contentType()))
        .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePrivate())
        .body(image.data());
  }

  @GetMapping("/filter-values")
  public java.util.Map<String, Object> getFilterValues() {
    return java.util.Map.of(
        "statuses", java.util.Arrays.stream(com.familywishes.entity.enums.EmailStatus.values()).map(Enum::name).toList(),
        "emailTypes", java.util.Arrays.stream(com.familywishes.entity.enums.EmailType.values()).map(Enum::name).toList());
  }

  @GetMapping("/status")
  public PagedResponse<EmailStatusResponse> status(
      Authentication authentication,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "") String searchKey,
      @RequestParam(defaultValue = "ALL") String mailTab,
      @RequestParam(required = false) java.util.List<com.familywishes.entity.enums.EmailStatus> statuses,
      @RequestParam(required = false) java.util.List<com.familywishes.entity.enums.EmailType> emailTypes,
      @RequestParam(defaultValue = "id") String sortBy,
      @RequestParam(defaultValue = "desc") String sortDir) {
    boolean isAdmin =
        authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch("ROLE_ADMIN"::equals);

    if ((statuses != null && !statuses.isEmpty()) || (emailTypes != null && !emailTypes.isEmpty())) {
      return emailService.getStatusWithFilters(
          page, size, searchKey, statuses, emailTypes, authentication.getName(), isAdmin, sortBy, sortDir);
    }

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

  @DeleteMapping("/status/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public void deleteWishStatus(@PathVariable Long id) {
    emailService.deleteEmailStatus(id);
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
