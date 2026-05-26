package com.familywishes.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class DashboardDtos {
  public record DashboardResponse(
      long totalUsers, long upcomingEvents, long emailsSentToday, long failedEmails) {}

  public record DashboardGraphPoint(String date, long sent, long failed, long total) {}

  public record DashboardGraphResponse(int days, List<DashboardGraphPoint> points) {}

  public record LoginLocationChartPoint(
          String location, String ipAddress, Double latitude, Double longitude, LocalDateTime loggedInAt) {}

  public record LoginLocationChartResponse(
      Long userId, String fromDate, String toDate, List<LoginLocationChartPoint> points) {}
}
