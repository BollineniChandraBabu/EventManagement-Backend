package com.familywishes.scheduler;

import com.familywishes.agent.DeliveryOpsAgent;
import com.familywishes.service.SchedulerTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryOpsAgentScheduler {
  private final DeliveryOpsAgent agent;
  private final SchedulerTrackingService schedulerTrackingService;

  @Value("${app.agent.delivery-ops.enabled:false}")
  private boolean enabled;

  @Scheduled(cron = "0 15,45 * * * ?", zone = "${scheduler.time-zone}")
  public void triage() {
    if (!enabled) {
      return;
    }
    schedulerTrackingService.track(
        "deliveryOpsAgent",
        () -> {
          // Skip the LLM call entirely when there is nothing to triage.
          if (agent.failedCount() == 0) {
            return;
          }
          String report = agent.run();
          log.info("Delivery ops report: {}", report);
        });
  }
}
