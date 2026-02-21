package com.familywishes.controller;

import com.familywishes.dto.DashboardDtos.DashboardGraphResponse;
import com.familywishes.dto.DashboardDtos.DashboardResponse;
import com.familywishes.service.DashboardService;
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
      Authentication authentication, @RequestParam(defaultValue = "7") int days) {
    return dashboardService.getInstaChart(days, authentication.getName(), isAdmin(authentication));
  }

  @GetMapping("/chart/mail")
  public DashboardGraphResponse mailChart(
      Authentication authentication, @RequestParam(defaultValue = "7") int days) {
    return dashboardService.getMailChart(days, authentication.getName(), isAdmin(authentication));
  }

  @GetMapping("/chart/insta")
  public DashboardGraphResponse instaChart(
      Authentication authentication, @RequestParam(defaultValue = "7") int days) {
    return dashboardService.getInstaChart(days, authentication.getName(), isAdmin(authentication));
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
  public DashboardGraphResponse otpMailChart(@RequestParam(defaultValue = "7") int days) {
    return dashboardService.getOtpChart(days);
  }

  @GetMapping("/chart/mail/forgot-password")
  @PreAuthorize("hasRole('ADMIN')")
  public DashboardGraphResponse forgotPasswordMailChart(@RequestParam(defaultValue = "7") int days) {
    return dashboardService.getForgotPasswordChart(days);
  }

  private boolean isAdmin(Authentication authentication) {
    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch("ROLE_ADMIN"::equals);
  }
}
