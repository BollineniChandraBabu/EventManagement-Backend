package com.familywishes.scheduler;

import com.familywishes.service.FestivalService;
import com.familywishes.service.SchedulerTrackingService;
import lombok.RequiredArgsConstructor;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FestivalSyncScheduler implements Job {

  private final FestivalService festivalService;
  private final SchedulerTrackingService schedulerTrackingService;

  @Override
  public void execute(JobExecutionContext context) {
    schedulerTrackingService.track(
        "festivalMonthlySyncScheduler", festivalService::syncCalendarificFestivals);
  }
}
