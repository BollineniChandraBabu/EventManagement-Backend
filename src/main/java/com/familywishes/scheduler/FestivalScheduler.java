package com.familywishes.scheduler;

import com.familywishes.dto.AiWishRequest;
import com.familywishes.dto.AiWishResponse;
import com.familywishes.entity.FestivalWishMapping;
import com.familywishes.entity.User;
import com.familywishes.entity.enums.EmailType;
import com.familywishes.repository.FestivalWishMappingRepository;
import com.familywishes.service.AiService;
import com.familywishes.service.GmailEmailService;
import com.familywishes.service.SchedulerTrackingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FestivalScheduler implements Job {

  private final FestivalWishMappingRepository mappingRepository;
  private final GmailEmailService gmailEmailService;
  private final SchedulerTrackingService schedulerTrackingService;
  private final AiService aiService;

  @Value("${alert.email.to}")
  private String alertEmail;

  @Value("${scheduler.time-zone:Asia/Kolkata}")
  private String schedulerTimeZone;

  @Override
  public void execute(JobExecutionContext context) {
    schedulerTrackingService.track("festivalWishScheduler", this::sendFestivalWishes);
  }

  private void sendFestivalWishes() {
    LocalDate today = LocalDate.now(ZoneId.of(schedulerTimeZone));

    List<FestivalWishMapping> mappings =
        mappingRepository.findBySpecialEvent_EventDateAndActiveTrue(today);

    if (mappings.isEmpty()) {
      return;
    }

    for (FestivalWishMapping mapping : mappings) {
      if (today.equals(mapping.getLastWishSentOn())) {
        continue;
      }

      try {
        sendFestivalWish(mapping);
      } catch (Exception e) {
        log.error(e.getMessage(), e);
        sendErrorEmail(mapping.getUser(), e);
      }

      mapping.setLastWishSentOn(today);
      mappingRepository.save(mapping);
      log.info(
          "Festival wish sent for event {} to user {}",
          mapping.getSpecialEvent().getEventName(),
          mapping.getUser().getEmail());
    }
  }

  private void sendFestivalWish(FestivalWishMapping festivalWishMapping)
      throws JsonProcessingException {
    AiWishRequest request =
        new AiWishRequest(
            festivalWishMapping.getUser().getName(),
            festivalWishMapping.getUser().getRelationShip().getCode(),
            "",
            festivalWishMapping.getSpecialEvent().getEventName(),
            "Emotional",
            "EN");
    AiWishResponse ai = aiService.generate(request);
    byte[] image = aiService.callGeminiImage(request);
    gmailEmailService.sendEmailWithAttachments(
        festivalWishMapping.getUser().getEmail(),
        ai.subject(),
        ai.htmlMessage(),
        null,
        image,
        EmailType.FESTIVAL_WISH);
    log.info("Birthday wish sent to {}", festivalWishMapping.getUser().getEmail());
  }

  private void sendErrorEmail(User user, Exception e) {
    gmailEmailService.sendEmailWithAttachments(
        alertEmail,
        "Festival Job Failed",
        "Failed for user: " + user.getEmail() + "<br>Error: " + e.getMessage(),
        null,
        null);
  }
}
