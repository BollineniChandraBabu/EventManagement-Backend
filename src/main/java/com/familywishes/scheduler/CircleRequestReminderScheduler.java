package com.familywishes.scheduler;

import com.familywishes.service.CircleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Daily nudge for circle requests that have sat unanswered for too long: emails the addressee every
 * day until they accept or reject, so a request never quietly goes stale.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CircleRequestReminderScheduler implements Job {

  private final CircleService circleService;

  @Value("${app.circle.request.reminder-after-days:7}")
  private int reminderAfterDays;

  @Override
  public void execute(JobExecutionContext context) {
    int sent = circleService.sendPendingReminders(reminderAfterDays);
    log.info(
        "Circle request reminder job finished: {} reminder(s) sent for requests pending over {} day(s)",
        sent,
        reminderAfterDays);
  }
}
