package com.familywishes.scheduler;

import com.familywishes.entity.MessageLog;
import com.familywishes.entity.MessageStatus;
import com.familywishes.repository.IGMessageLogRepository;
import com.familywishes.service.SchedulerTrackingService;
import com.familywishes.service.impl.MessageDispatcher;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RetryScheduler {

  private final IGMessageLogRepository repo;
  private final MessageDispatcher dispatcher;
  private final SchedulerTrackingService schedulerTrackingService;

  @Scheduled(cron = "0 */15 * * * ?", zone = "${scheduler.time-zone}")
  public void retryFailed() {
    schedulerTrackingService.track(
        "instagramRetryScheduler",
        () -> {
          List<MessageLog> failed = repo.findByStatusAndRetryCountLessThan(MessageStatus.FAILED, 3);

          for (MessageLog log : failed) {
            dispatcher.sendAsync(log);
          }
        });
  }
}
