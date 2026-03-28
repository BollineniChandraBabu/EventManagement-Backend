package com.familywishes.scheduler;

import com.familywishes.entity.FestivalWishMapping;
import com.familywishes.entity.MessageLog;
import com.familywishes.entity.MessageStatus;
import com.familywishes.repository.FestivalWishMappingRepository;
import com.familywishes.repository.IGMessageLogRepository;
import com.familywishes.service.SchedulerTrackingService;
import com.familywishes.service.impl.MessageDispatcher;
import com.familywishes.util.TimeUtil;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FestivalScheduler {

  private final FestivalWishMappingRepository mappingRepository;
  private final IGMessageLogRepository logRepo;
  private final MessageDispatcher dispatcher;
  private final TimeUtil timeUtil;
  private final SchedulerTrackingService schedulerTrackingService;

  @Scheduled(cron = "0 30 7 * * ?", zone = "${scheduler.time-zone}")
  public void sendFestivalWishes() {
    schedulerTrackingService.track(
        "instagramFestivalScheduler",
        () -> {
          LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));

          List<FestivalWishMapping> mappings =
              mappingRepository.findBySpecialEvent_EventDateAndActiveTrue(today);

          if (mappings.isEmpty()) return;

          for (FestivalWishMapping mapping : mappings) {
            if (timeUtil.canSend(mapping.getInstagramUser().getLastUserMessageTime())) {
              MessageLog log = new MessageLog();
              log.setInstagramUserId(mapping.getInstagramUser().getInstagramUserId());
              log.setMessage(
                  mapping.getCustomMessage() != null && !mapping.getCustomMessage().isBlank()
                      ? mapping.getCustomMessage()
                      : mapping.getSpecialEvent().getMessage());
              log.setStatus(MessageStatus.PENDING);
              log.setCreatedAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));

              logRepo.save(log);
              dispatcher.sendAsync(log);
            }
          }
        });
  }
}
