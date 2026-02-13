package com.familywishes.dto;

public class DashboardDtos {
    public record DashboardResponse(long totalUsers, long upcomingEvents, long emailsSentToday, long failedEmails) {}
}
