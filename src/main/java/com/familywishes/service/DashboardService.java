package com.familywishes.service;

import com.familywishes.dto.DashboardDtos.DashboardGraphResponse;
import com.familywishes.dto.DashboardDtos.LoginLocationChartResponse;
import com.familywishes.dto.DashboardDtos.DashboardResponse;
import java.time.LocalDate;

public interface DashboardService {
  DashboardResponse getDashboard(String requesterEmail, boolean isAdmin);

  DashboardResponse getIGDashboard(String requesterEmail, boolean isAdmin);

  DashboardGraphResponse getMailChart(LocalDate startDate, LocalDate endDate, String requesterEmail, boolean isAdmin);

  DashboardGraphResponse getInstaChart(LocalDate startDate, LocalDate endDate, String requesterEmail, boolean isAdmin);

  DashboardResponse getOtpDashboard();

  DashboardResponse getForgotPasswordDashboard();

  DashboardGraphResponse getOtpChart(LocalDate startDate, LocalDate endDate);

  DashboardGraphResponse getForgotPasswordChart(LocalDate startDate, LocalDate endDate);

  LoginLocationChartResponse getLoginLocationChart(
      Long userId, String fromDate, String toDate, String requesterEmail, boolean isAdmin);
}
