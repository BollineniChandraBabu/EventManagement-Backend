package com.familywishes.scheduler;

import com.familywishes.entity.FestivalWishMapping;
import com.familywishes.repository.FestivalWishMappingRepository;
import com.familywishes.service.GmailEmailService;
import com.familywishes.service.SchedulerTrackingService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FestivalScheduler implements Job {

  private final FestivalWishMappingRepository mappingRepository;
  private final GmailEmailService gmailEmailService;
  private final SchedulerTrackingService schedulerTrackingService;

  @Override
  public void execute(JobExecutionContext context) {
    schedulerTrackingService.track("festivalWishScheduler", this::sendFestivalWishes);
  }

  void sendFestivalWishes() {
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));

    List<FestivalWishMapping> mappings =
        mappingRepository.findBySpecialEvent_EventDateAndActiveTrue(today);

    if (mappings.isEmpty()) {
      return;
    }

    for (FestivalWishMapping mapping : mappings) {
      if (today.equals(mapping.getLastWishSentOn())) {
        continue;
      }

      String body =
          mapping.getCustomMessage() != null && !mapping.getCustomMessage().isBlank()
              ? mapping.getCustomMessage()
              : mapping.getSpecialEvent().getMessage();

      gmailEmailService.sendEmailWithAttachments(
          mapping.getUser().getEmail(), "Happy " + mapping.getSpecialEvent().getEventName(), body, null, null);

      mapping.setLastWishSentOn(today);
      mappingRepository.save(mapping);
      log.info(
          "Festival wish sent for event {} to user {}",
          mapping.getSpecialEvent().getEventName(),
          mapping.getUser().getEmail());
    }
  }
}
