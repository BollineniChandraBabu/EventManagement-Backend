package com.familywishes.dto;

import java.util.List;

public class DashboardDtos {
  public record DashboardResponse(
      long totalUsers, long upcomingEvents, long emailsSentToday, long failedEmails) {}

  public record DashboardGraphPoint(String date, long sent, long failed, long total) {}

  public record DashboardGraphResponse(int days, List<DashboardGraphPoint> points) {}
}
