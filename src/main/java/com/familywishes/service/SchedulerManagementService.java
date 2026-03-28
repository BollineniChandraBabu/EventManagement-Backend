package com.familywishes.service;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.SchedulerDtos.SchedulerStatusResponse;
import com.familywishes.dto.SchedulerDtos.SchedulerTriggerResponse;
import com.familywishes.exception.NotFoundException;
import com.familywishes.scheduler.BirthdayScheduler;
import com.familywishes.scheduler.EmailAlertScheduler;
import com.familywishes.scheduler.RetryScheduler;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SchedulerManagementService {

  private final Scheduler quartzScheduler;
  private final SchedulerTrackingService trackingService;
  private final BirthdayScheduler birthdayScheduler;
  private final RetryScheduler retryScheduler;
  private final EmailAlertScheduler emailAlertScheduler;

  @Value("${scheduler.time-zone:Asia/Kolkata}")
  private String schedulerTimeZone;

  private record ManualSchedulerDefinition(Runnable action, String cron, ZoneId zoneId) {}

  private Map<String, ManualSchedulerDefinition> manualJobs() {
    ZoneId schedulerZone = ZoneId.of(schedulerTimeZone);

    Map<String, ManualSchedulerDefinition> jobs = new LinkedHashMap<>();
    jobs.put(
        "instagramBirthdayScheduler",
        new ManualSchedulerDefinition(
            birthdayScheduler::sendBirthdayWishes, "0 0 7 * * ?", schedulerZone));
    jobs.put(
        "instagramRetryScheduler",
        new ManualSchedulerDefinition(
            retryScheduler::retryFailed, "0 */15 * * * ?", schedulerZone));
    jobs.put(
        "instagramEmailAlertScheduler",
        new ManualSchedulerDefinition(
            emailAlertScheduler::checkFailures, "0 */30 * * * ?", schedulerZone));
    return jobs;
  }

  public PagedResponse<SchedulerStatusResponse> getAllStatuses(
      int page, int size, String searchKey, String sortBy, String sortDir) {
    List<SchedulerStatusResponse> statuses = new ArrayList<>();

    try {
      for (JobKey jobKey : quartzScheduler.getJobKeys(GroupMatcher.anyGroup())) {
        statuses.add(buildQuartzStatus(jobKey));
      }
    } catch (SchedulerException e) {
      throw new RuntimeException("Failed to load quartz jobs", e);
    }

    manualJobs()
        .forEach((jobName, definition) -> statuses.add(buildManualStatus(jobName, definition)));

    Comparator<SchedulerStatusResponse> comparator = resolveComparator(sortBy);
    if (!"asc".equalsIgnoreCase(sortDir)) {
      comparator = comparator.reversed();
    }
    statuses.sort(comparator);

    String normalizedSearchKey = searchKey == null ? "" : searchKey.trim().toLowerCase(Locale.ROOT);
    List<SchedulerStatusResponse> filteredStatuses =
        statuses.stream()
            .filter(
                status ->
                    normalizedSearchKey.isEmpty()
                        || status.name().toLowerCase(Locale.ROOT).contains(normalizedSearchKey)
                        || status.type().toLowerCase(Locale.ROOT).contains(normalizedSearchKey))
            .toList();

    int safePage = Math.max(page, 0);
    int safeSize = Math.max(size, 1);
    int fromIndex = Math.min(safePage * safeSize, filteredStatuses.size());
    int toIndex = Math.min(fromIndex + safeSize, filteredStatuses.size());
    List<SchedulerStatusResponse> content = filteredStatuses.subList(fromIndex, toIndex);
    int totalPages =
        filteredStatuses.isEmpty()
            ? 0
            : (int) Math.ceil((double) filteredStatuses.size() / safeSize);

    return new PagedResponse<>(
        content,
        safePage,
        safeSize,
        filteredStatuses.size(),
        totalPages,
        safePage + 1 < totalPages,
        safePage > 0);
  }

  public SchedulerStatusResponse getStatusByName(String jobName) {
    Map<String, ManualSchedulerDefinition> manualJobs = manualJobs();
    if (manualJobs.containsKey(jobName)) {
      return buildManualStatus(jobName, manualJobs.get(jobName));
    }

    try {
      return buildQuartzStatus(resolveQuartzJobKey(jobName));
    } catch (SchedulerException e) {
      throw new RuntimeException("Failed to load quartz scheduler: " + jobName, e);
    }
  }

  public SchedulerTriggerResponse trigger(String jobName) {
    Map<String, ManualSchedulerDefinition> manualJobs = manualJobs();

    if (manualJobs.containsKey(jobName)) {
      manualJobs.get(jobName).action().run();
      return new SchedulerTriggerResponse(jobName, "Manual scheduler triggered", Instant.now());
    }

    try {
      JobKey key = resolveQuartzJobKey(jobName);
      quartzScheduler.triggerJob(key);
      return new SchedulerTriggerResponse(jobName, "Quartz scheduler triggered", Instant.now());
    } catch (SchedulerException e) {
      throw new RuntimeException("Failed to trigger scheduler: " + jobName, e);
    }
  }

  private JobKey resolveQuartzJobKey(String jobName) throws SchedulerException {
    for (JobKey key : quartzScheduler.getJobKeys(GroupMatcher.anyGroup())) {
      if (key.getName().equals(jobName)) {
        return key;
      }
    }
    throw new NotFoundException("Scheduler not found: " + jobName);
  }

  private SchedulerStatusResponse buildQuartzStatus(JobKey key) {
    SchedulerTrackingService.SchedulerExecutionStats stats =
        trackingService.getStats(key.getName());
    Instant nextFire = null;
    Instant previousFire = null;

    try {
      List<? extends Trigger> triggers = quartzScheduler.getTriggersOfJob(key);
      if (!triggers.isEmpty()) {
        Trigger trigger = triggers.get(0);
        if (trigger.getNextFireTime() != null) {
          nextFire = trigger.getNextFireTime().toInstant();
        }
        if (trigger.getPreviousFireTime() != null) {
          previousFire = trigger.getPreviousFireTime().toInstant();
        }
      }
    } catch (SchedulerException e) {
      throw new RuntimeException("Failed to read trigger for scheduler: " + key.getName(), e);
    }

    return fromStats(key.getName(), "QUARTZ", stats, nextFire, previousFire);
  }

  private SchedulerStatusResponse buildManualStatus(
      String jobName, ManualSchedulerDefinition definition) {
    SchedulerTrackingService.SchedulerExecutionStats stats = trackingService.getStats(jobName);
    Instant previousFire = stats != null ? stats.getLastStartedAt() : null;
    Instant nextFire = calculateNextFireTime(definition.cron(), definition.zoneId());

    return fromStats(jobName, "SPRING_SCHEDULED", stats, nextFire, previousFire);
  }

  private Instant calculateNextFireTime(String cron, ZoneId zoneId) {
    CronExpression cronExpression = CronExpression.parse(cron);
    ZonedDateTime now = ZonedDateTime.now(zoneId);
    ZonedDateTime next = cronExpression.next(now);
    return next != null ? next.toInstant() : null;
  }

  private Comparator<SchedulerStatusResponse> resolveComparator(String sortBy) {
    String normalizedSortBy = sortBy == null ? "name" : sortBy.trim().toLowerCase(Locale.ROOT);

    return switch (normalizedSortBy) {
      case "type" ->
          Comparator.comparing(SchedulerStatusResponse::type, String.CASE_INSENSITIVE_ORDER);
      case "totalruns" -> Comparator.comparingLong(SchedulerStatusResponse::totalRuns);
      case "successruns" -> Comparator.comparingLong(SchedulerStatusResponse::successRuns);
      case "failedruns" -> Comparator.comparingLong(SchedulerStatusResponse::failedRuns);
      case "nextfiretime" ->
          Comparator.comparing(
              SchedulerStatusResponse::nextFireTime,
              Comparator.nullsLast(Comparator.naturalOrder()));
      case "previousfiretime" ->
          Comparator.comparing(
              SchedulerStatusResponse::previousFireTime,
              Comparator.nullsLast(Comparator.naturalOrder()));
      default -> Comparator.comparing(SchedulerStatusResponse::name, String.CASE_INSENSITIVE_ORDER);
    };
  }

  private SchedulerStatusResponse fromStats(
      String name,
      String type,
      SchedulerTrackingService.SchedulerExecutionStats stats,
      Instant nextFire,
      Instant previousFire) {
    return new SchedulerStatusResponse(
        name,
        type,
        stats != null && stats.isRunning(),
        stats != null ? stats.getTotalRuns().get() : 0,
        stats != null ? stats.getSuccessRuns().get() : 0,
        stats != null ? stats.getFailedRuns().get() : 0,
        stats != null ? stats.getLastStartedAt() : null,
        stats != null ? stats.getLastCompletedAt() : null,
        stats != null ? stats.getLastDurationMs() : null,
        stats != null ? stats.getLastStatus() : null,
        stats != null ? stats.getLastError() : null,
        nextFire,
        previousFire);
  }
}
