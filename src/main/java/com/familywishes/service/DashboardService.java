package com.familywishes.service;

import com.familywishes.dto.DashboardDtos.DashboardGraphResponse;
import com.familywishes.dto.DashboardDtos.LoginLocationChartResponse;
import com.familywishes.dto.DashboardDtos.DashboardResponse;

public interface DashboardService {
  DashboardResponse getDashboard(String requesterEmail, boolean isAdmin);

  DashboardResponse getIGDashboard(String requesterEmail, boolean isAdmin);

  DashboardGraphResponse getMailChart(int days, String requesterEmail, boolean isAdmin);

  DashboardGraphResponse getInstaChart(int days, String requesterEmail, boolean isAdmin);

  DashboardResponse getOtpDashboard();

  DashboardResponse getForgotPasswordDashboard();

  DashboardGraphResponse getOtpChart(int days);

  DashboardGraphResponse getForgotPasswordChart(int days);

  LoginLocationChartResponse getLoginLocationChart(Long userId, String fromDate, String toDate);
}
