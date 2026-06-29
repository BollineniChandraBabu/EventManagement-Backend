package com.familywishes.controller;

import com.familywishes.chat.ChatDtos;
import com.familywishes.chat.ChatService;
import com.familywishes.dto.DashboardDtos.DashboardGraphResponse;
import com.familywishes.dto.DashboardDtos.DashboardResponse;
import com.familywishes.dto.DashboardDtos.LoginLocationChartResponse;
import com.familywishes.dto.DashboardDtos.ViolatedUsersDashboardResponse;
import com.familywishes.exception.BadRequestException;
import com.familywishes.service.DashboardService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
  private final DashboardService dashboardService;
  private final ChatService chatService;

  @GetMapping(value = "/mail")
  public DashboardResponse getMailDashboard(Authentication authentication) {
    return dashboardService.getDashboard(authentication.getName(), isAdmin(authentication));
  }

  @GetMapping(value = "/insta")
  public DashboardResponse getIGDashboard(Authentication authentication) {
    return dashboardService.getIGDashboard(authentication.getName(), isAdmin(authentication));
  }

  @GetMapping("/chart")
  public DashboardGraphResponse chart(
      Authentication authentication,
      @RequestParam String startDate,
      @RequestParam String endDate) {
    DateRange dateRange = validateDateRange(startDate, endDate);
    return dashboardService.getInstaChart(
        dateRange.startDate(),
        dateRange.endDate(),
        authentication.getName(),
        isAdmin(authentication));
  }

  @GetMapping("/chart/mail")
  public DashboardGraphResponse mailChart(
      Authentication authentication,
      @RequestParam String startDate,
      @RequestParam String endDate) {
    DateRange dateRange = validateDateRange(startDate, endDate);
    return dashboardService.getMailChart(
        dateRange.startDate(),
        dateRange.endDate(),
        authentication.getName(),
        isAdmin(authentication));
  }

  @GetMapping("/chart/insta")
  public DashboardGraphResponse instaChart(
      Authentication authentication,
      @RequestParam String startDate,
      @RequestParam String endDate) {
    DateRange dateRange = validateDateRange(startDate, endDate);
    return dashboardService.getInstaChart(
        dateRange.startDate(),
        dateRange.endDate(),
        authentication.getName(),
        isAdmin(authentication));
  }

  @GetMapping("/mail/otp")
  @PreAuthorize("hasRole('ADMIN')")
  public DashboardResponse otpMailDashboard() {
    return dashboardService.getOtpDashboard();
  }

  @GetMapping("/mail/forgot-password")
  @PreAuthorize("hasRole('ADMIN')")
  public DashboardResponse forgotPasswordMailDashboard() {
    return dashboardService.getForgotPasswordDashboard();
  }

  @GetMapping("/chart/mail/otp")
  @PreAuthorize("hasRole('ADMIN')")
  public DashboardGraphResponse otpMailChart(
      @RequestParam String startDate,
      @RequestParam String endDate) {
    DateRange dateRange = validateDateRange(startDate, endDate);
    return dashboardService.getOtpChart(dateRange.startDate(), dateRange.endDate());
  }

  @GetMapping("/chart/mail/forgot-password")
  @PreAuthorize("hasRole('ADMIN')")
  public DashboardGraphResponse forgotPasswordMailChart(
      @RequestParam String startDate,
      @RequestParam String endDate) {
    DateRange dateRange = validateDateRange(startDate, endDate);
    return dashboardService.getForgotPasswordChart(dateRange.startDate(), dateRange.endDate());
  }

  @GetMapping("/chart/login-locations")
  public LoginLocationChartResponse loginLocationChart(
          Authentication authentication,
          @RequestParam(required = false) Long userId,
          @RequestParam(required = false) String startDate,
          @RequestParam(required = false) String endDate,
          @RequestParam(required = false) List<Long> userIds) {
    validateDateRange(startDate, endDate);
    return dashboardService.getLoginLocationChart(
        userId, startDate, endDate, authentication.getName(), isAdmin(authentication), userIds);
  }

  @GetMapping("/violated-users")
  @PreAuthorize("hasRole('ADMIN')")
  public ViolatedUsersDashboardResponse violatedUsersDashboard(
      @RequestParam String startDate,
      @RequestParam String endDate,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size,
      @RequestParam(defaultValue = "true") boolean includeIpInfo) {
    if (page < 0) {
      throw new BadRequestException("page must not be negative");
    }
    if (size < 1 || size > 500) {
      throw new BadRequestException("size must be between 1 and 500");
    }
    DateRange dateRange = validateInclusiveDateRange(startDate, endDate);
    return dashboardService.getViolatedUsersDashboard(
        dateRange.startDate(), dateRange.endDate(), page, size, includeIpInfo);
  }

  @GetMapping("/chat/messages")
  public ChatDtos.GlobalMessagePageResponse chatMessages(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size,
      @RequestParam(defaultValue = "") String searchKey) {
    return chatService.listAllMessages(page, size, searchKey);
  }

  @GetMapping("/chat/users/active")
  public java.util.List<ChatDtos.ChatUserResponse> activeChatUsers() {
    return chatService.listActiveUsers();
  }

  private DateRange validateInclusiveDateRange(String startDateParam, String endDateParam) {
    DateRange dateRange = parseDateRange(startDateParam, endDateParam);
    LocalDate today = LocalDate.now(ZoneId.systemDefault());
    if (dateRange.endDate().isAfter(today)) {
      throw new BadRequestException("endDate must not be greater than today");
    }
    if (dateRange.startDate().isAfter(dateRange.endDate())) {
      throw new BadRequestException("startDate must be before or equal to endDate");
    }
    return dateRange;
  }

  private DateRange validateDateRange(String startDateParam, String endDateParam) {
    DateRange dateRange = parseDateRange(startDateParam, endDateParam);
    LocalDate startDate = dateRange.startDate();
    LocalDate endDate = dateRange.endDate();

    LocalDate today = LocalDate.now(ZoneId.systemDefault());
    if (endDate.isAfter(today)) {
      throw new BadRequestException("endDate must not be greater than today");
    }
    if (startDate.isEqual(endDate)) {
      throw new BadRequestException("startDate and endDate must not be the same");
    }
    if (startDate.isAfter(endDate)) {
      throw new BadRequestException("startDate must be before endDate");
    }

    return dateRange;
  }

  private DateRange parseDateRange(String startDateParam, String endDateParam) {
    LocalDate startDate;
    LocalDate endDate;

    try {
      startDate = LocalDate.parse(startDateParam);
      endDate = LocalDate.parse(endDateParam);
    } catch (Exception ex) {
      throw new BadRequestException("Invalid date format. Use yyyy-MM-dd for startDate and endDate");
    }

    return new DateRange(startDate, endDate);
  }

  private record DateRange(LocalDate startDate, LocalDate endDate) {}

  private boolean isAdmin(Authentication authentication) {
    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch("ROLE_ADMIN"::equals);
  }
}
