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
import com.familywishes.util.WishEmailExtras;
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
  private final com.familywishes.service.impl.SupabaseStorageService storageService;

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
      if (today.equals(mapping.getLastWishSentOn()) && !mapping.isError()) {
        continue;
      }

      boolean sent = sendOrReport(mapping);
      // The flag is cleared on success; otherwise a mapping that failed once would be sent again
      // on every run of the day. A failed one keeps the flag so a later run retries it.
      mapping.setError(!sent);
      mapping.setLastWishSentOn(today);
      mappingRepository.save(mapping);
    }
  }

  private boolean sendOrReport(FestivalWishMapping mapping) {
    try {
      sendFestivalWish(mapping);
      return true;
    } catch (Exception e) {
      log.error(
          "Festival wish for {} failed for user {}",
          mapping.getSpecialEvent().getEventName(),
          mapping.getUser().getEmail(),
          e);
      sendErrorEmail(mapping.getUser(), e);
      return false;
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
            "EN",
            festivalWishMapping.getUser().getId());
    AiWishResponse ai = aiService.generate(request);

    // If an image is specifically available for this festival mapping, use it; else generate/resolve a new image
    byte[] image = null;
    if (festivalWishMapping.getImageObjectKey() != null
        && !festivalWishMapping.getImageObjectKey().isBlank()) {
      image = storageService.downloadImage(festivalWishMapping.getImageObjectKey());
    }
    if (image == null || image.length == 0) {
      image = aiService.callGeminiImage(request);
    }

    // The personal note is only added to mappings that have one.
    String body = WishEmailExtras.appendPersonalNote(ai.htmlMessage(), festivalWishMapping.getCustomMessage());
    gmailEmailService.sendEmailWithAttachments(
        festivalWishMapping.getUser().getEmail(),
        ai.subject(),
        body,
        null,
        image,
        EmailType.FESTIVAL_WISH);
    log.info(
        "Festival wish for {} sent to {}",
        festivalWishMapping.getSpecialEvent().getEventName(),
        festivalWishMapping.getUser().getEmail());
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
