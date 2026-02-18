package com.familywishes.service;

import com.familywishes.dto.SchedulerDtos.SchedulerStatusResponse;
import com.familywishes.dto.SchedulerDtos.SchedulerTriggerResponse;
import com.familywishes.exception.NotFoundException;
import com.familywishes.scheduler.BirthdayScheduler;
import com.familywishes.scheduler.EmailAlertScheduler;
import com.familywishes.scheduler.FestivalScheduler;
import com.familywishes.scheduler.RetryScheduler;
import lombok.RequiredArgsConstructor;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SchedulerManagementService {

    private final Scheduler quartzScheduler;
    private final SchedulerTrackingService trackingService;
    private final BirthdayScheduler birthdayScheduler;
    private final FestivalScheduler festivalScheduler;
    private final RetryScheduler retryScheduler;
    private final EmailAlertScheduler emailAlertScheduler;

    private Map<String, Runnable> manualJobs() {
        Map<String, Runnable> jobs = new LinkedHashMap<>();
        jobs.put("instagramBirthdayScheduler", birthdayScheduler::sendBirthdayWishes);
        jobs.put("instagramFestivalScheduler", festivalScheduler::sendFestivalWishes);
        jobs.put("instagramRetryScheduler", retryScheduler::retryFailed);
        jobs.put("instagramEmailAlertScheduler", emailAlertScheduler::checkFailures);
        return jobs;
    }

    public List<SchedulerStatusResponse> getAllStatuses() {
        List<SchedulerStatusResponse> statuses = new ArrayList<>();

        try {
            for (JobKey jobKey : quartzScheduler.getJobKeys(GroupMatcher.anyGroup())) {
                statuses.add(buildQuartzStatus(jobKey));
            }
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to load quartz jobs", e);
        }

        manualJobs().keySet().forEach(jobName ->
                statuses.add(buildManualStatus(jobName))
        );

        statuses.sort(Comparator.comparing(SchedulerStatusResponse::name));
        return statuses;
    }

    public SchedulerTriggerResponse trigger(String jobName) {
        if (manualJobs().containsKey(jobName)) {
            manualJobs().get(jobName).run();
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
        SchedulerTrackingService.SchedulerExecutionStats stats = trackingService.getStats(key.getName());
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

    private SchedulerStatusResponse buildManualStatus(String jobName) {
        SchedulerTrackingService.SchedulerExecutionStats stats = trackingService.getStats(jobName);
        return fromStats(jobName, "SPRING_SCHEDULED", stats, null, null);
    }

    private SchedulerStatusResponse fromStats(
            String name,
            String type,
            SchedulerTrackingService.SchedulerExecutionStats stats,
            Instant nextFire,
            Instant previousFire
    ) {
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
                previousFire
        );
    }
}
