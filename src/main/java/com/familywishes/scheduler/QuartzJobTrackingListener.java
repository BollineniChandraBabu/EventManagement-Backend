package com.familywishes.scheduler;

import com.familywishes.service.SchedulerTrackingService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.listeners.JobListenerSupport;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class QuartzJobTrackingListener extends JobListenerSupport {

  private final Scheduler quartzScheduler;
  private final SchedulerTrackingService trackingService;

  @Override
  public String getName() {
    return "quartz-job-tracking-listener";
  }

  @PostConstruct
  public void register() {
    try {
      quartzScheduler.getListenerManager().addJobListener(this);
    } catch (SchedulerException e) {
      log.error(e.getMessage(), e);
      throw new RuntimeException("Failed to register quartz tracking listener", e);
    }
  }

  @Override
  public void jobToBeExecuted(JobExecutionContext context) {
    trackingService.onStart(context.getJobDetail().getKey().getName());
  }

  @Override
  public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
    trackingService.onComplete(context.getJobDetail().getKey().getName(), jobException);
  }
}
