package com.familywishes.dto;

import java.time.Instant;

public class SchedulerDtos {
    public record SchedulerStatusResponse(
            String name,
            String type,
            boolean running,
            Long totalRuns,
            Long successRuns,
            Long failedRuns,
            Instant lastStartedAt,
            Instant lastCompletedAt,
            Long lastDurationMs,
            String lastStatus,
            String lastError,
            Instant nextFireTime,
            Instant previousFireTime
    ) {
    }

    public record SchedulerTriggerResponse(
            String name,
            String message,
            Instant triggeredAt
    ) {
    }
}
