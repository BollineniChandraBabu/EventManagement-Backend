package com.familywishes.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SchedulerTrackingService {

  private final Map<String, SchedulerExecutionStats> statsByJob = new ConcurrentHashMap<>();

  public void onStart(String jobName) {
    SchedulerExecutionStats stats = getOrCreate(jobName);
    stats.running = true;
    stats.lastStartedAt = Instant.now();
  }

  public void onComplete(String jobName, Throwable error) {
    SchedulerExecutionStats stats = getOrCreate(jobName);
    Instant completedAt = Instant.now();
    stats.running = false;
    stats.lastCompletedAt = completedAt;
    stats.totalRuns.incrementAndGet();

    if (stats.lastStartedAt != null) {
      stats.lastDurationMs = Duration.between(stats.lastStartedAt, completedAt).toMillis();
    }

    if (error == null) {
      stats.successRuns.incrementAndGet();
      stats.lastStatus = "SUCCESS";
      stats.lastError = null;
    } else {
      stats.failedRuns.incrementAndGet();
      stats.lastStatus = "FAILED";
      stats.lastError = error.getMessage();
    }
  }

  public void track(String jobName, Runnable action) {
    onStart(jobName);
    try {
      action.run();
      onComplete(jobName, null);
    } catch (RuntimeException ex) {
      log.error(ex.getMessage(), ex);
      onComplete(jobName, ex);
      throw ex;
    }
  }

  public SchedulerExecutionStats getStats(String jobName) {
    return statsByJob.get(jobName);
  }

  private SchedulerExecutionStats getOrCreate(String jobName) {
    return statsByJob.computeIfAbsent(jobName, key -> new SchedulerExecutionStats());
  }

  @Getter
  public static class SchedulerExecutionStats {
    private volatile boolean running;
    private final AtomicLong totalRuns = new AtomicLong();
    private final AtomicLong successRuns = new AtomicLong();
    private final AtomicLong failedRuns = new AtomicLong();
    private volatile Instant lastStartedAt;
    private volatile Instant lastCompletedAt;
    private volatile Long lastDurationMs;
    private volatile String lastStatus;
    private volatile String lastError;
  }
}
