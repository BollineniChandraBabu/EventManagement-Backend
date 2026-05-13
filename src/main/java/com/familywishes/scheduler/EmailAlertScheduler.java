package com.familywishes.scheduler;

import com.familywishes.entity.MessageStatus;
import com.familywishes.repository.IGMessageLogRepository;
import com.familywishes.service.EmailService;
import com.familywishes.service.SchedulerTrackingService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmailAlertScheduler {

  private final IGMessageLogRepository repo;
  private final EmailService emailService;
  private final SchedulerTrackingService schedulerTrackingService;

  @Value("${scheduler.time-zone:Asia/Kolkata}")
  private String schedulerTimeZone;

  public EmailAlertScheduler(
      IGMessageLogRepository repo,
      EmailService emailService,
      SchedulerTrackingService schedulerTrackingService) {
    this.repo = repo;
    this.emailService = emailService;
    this.schedulerTrackingService = schedulerTrackingService;
  }

  @Scheduled(cron = "0 */30 * * * ?", zone = "${scheduler.time-zone}")
  public void checkFailures() {
    schedulerTrackingService.track(
        "instagramEmailAlertScheduler",
        () -> {
          LocalDateTime start = LocalDate.now(ZoneId.of(schedulerTimeZone)).atStartOfDay();

          long failed =
              repo.countByStatusAndCreatedAtBetween(
                  MessageStatus.FAILED, start, LocalDateTime.now(ZoneId.of(schedulerTimeZone)));

          if (failed > 5) {
            emailService.sendFailureAlert(failed);
          }
        });
  }
}
