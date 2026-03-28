package com.familywishes.scheduler;

import com.familywishes.service.FestivalService;
import com.familywishes.service.SchedulerTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FestivalSyncScheduler {

  private final FestivalService festivalService;
  private final SchedulerTrackingService schedulerTrackingService;

  @Scheduled(cron = "0 0 2 1 * ?", zone = "${scheduler.time-zone}")
  public void syncFestivals() {
    schedulerTrackingService.track("festivalMonthlySyncScheduler", festivalService::syncCalendarificFestivals);
  }
}
