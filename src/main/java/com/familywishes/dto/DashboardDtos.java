package com.familywishes.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class DashboardDtos {
  /** Serializable because it is kept in the dashboardSummary cache. */
  public record DashboardResponse(
      long totalUsers, long upcomingEvents, long emailsSentToday, long failedEmails)
      implements Serializable {}

  public record DashboardGraphPoint(String date, long sent, long failed, long total) {}

  public record DashboardGraphResponse(int days, List<DashboardGraphPoint> points) {}

  public record LoginLocationChartPoint(
         String email, String userName, String location, String ipAddress, Double latitude, Double longitude, LocalDateTime loggedInAt) {}

  /** truncated = older logins in the range were left out (only the newest ones are returned). */
  public record LoginLocationChartResponse(
      Long userId,
      String fromDate,
      String toDate,
      List<LoginLocationChartPoint> points,
      boolean truncated) {}

  public record ViolatedUserChartPoint(String date, long total) {}

  public record ViolatedUserIpInfo(
      String ipAddress,
      String status,
      String message,
      String country,
      String countryCode,
      String region,
      String regionName,
      String city,
      String zip,
      Double latitude,
      Double longitude,
      String timezone,
      String isp,
      String organization,
      String asn,
      Boolean mobile,
      Boolean proxy,
      Boolean hosting,
      Map<String, Object> raw)
      implements Serializable {} // kept in the ipInfo cache

  public record ViolatedUserInfo(
      Long id,
      String email,
      String password,
      String loginLocation,
      String ipAddress,
      Double latitude,
      Double longitude,
      LocalDateTime loggedInAt,
      ViolatedUserIpInfo ipInfo) {}

  public record ViolatedUserMapPoint(
      Long id,
      String email,
      String loginLocation,
      String ipAddress,
      Double latitude,
      Double longitude,
      LocalDateTime loggedInAt,
      ViolatedUserIpInfo ipInfo) {}

  public record ViolatedUsersDashboardResponse(
      String startDate,
      String endDate,
      long totalAttempts,
      List<ViolatedUserChartPoint> chartPoints,
      List<ViolatedUserMapPoint> mapPoints,
      List<ViolatedUserInfo> users,
      int page,
      int size,
      long totalElements,
      int totalPages,
      boolean hasNext,
      boolean hasPrevious) {}
}
