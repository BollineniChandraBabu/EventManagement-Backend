package com.familywishes.scheduler;

import com.familywishes.dto.AiWishRequest;
import com.familywishes.dto.AiWishResponse;
import com.familywishes.entity.WishSeedTemplate;
import com.familywishes.entity.enums.EventType;
import com.familywishes.entity.enums.SeedTemplateType;
import com.familywishes.repository.EventRepository;
import com.familywishes.repository.WishSeedTemplateRepository;
import com.familywishes.service.AiService;
import com.familywishes.service.GmailEmailService;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WishesScheduler implements Job {
  private final EventRepository eventRepository;
  private final WishSeedTemplateRepository wishSeedTemplateRepository;
  private final GmailEmailService emailService;
  private final AiService aiService;

  @Override
  public void execute(JobExecutionContext context) {
    WishSeedTemplate festivalTemplate =
        wishSeedTemplateRepository
            .findByTypeAndActiveTrue(SeedTemplateType.FESTIVAL)
            .orElse(defaultFestivalTemplate());

    eventRepository
        .findByEventDateAndActiveTrue(LocalDate.now(ZoneId.of("Asia/Kolkata")))
        .forEach(
            event -> {
              AiWishResponse ai;
              try {
                AiWishRequest request =
                    buildRequest(
                        event.getUser().getName(),
                        event.getEventType(),
                        event.getFestivalName(),
                        festivalTemplate);
                ai = aiService.generate(request);
              } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
              }

              emailService.sendEmailWithAttachments(
                  event.getUser().getEmail(), ai.subject(), ai.htmlMessage(), null, null);
            });
    emailService.retryFailed();
    log.info("Daily wish job completed");
  }

  private AiWishRequest buildRequest(
      String name, EventType eventType, String festivalName, WishSeedTemplate festivalTemplate) {
    if (eventType == EventType.FESTIVAL) {
      return new AiWishRequest(
          name,
          festivalTemplate.getRelation(),
          festivalTemplate.getEvent(),
          festivalName,
          festivalTemplate.getTone(),
          festivalTemplate.getLanguage());
    }

    return new AiWishRequest(name, "Family", eventType.name(), festivalName, "Emotional", "EN");
  }

  private WishSeedTemplate defaultFestivalTemplate() {
    return WishSeedTemplate.builder()
        .type(SeedTemplateType.FESTIVAL)
        .relation("Family")
        .event("FESTIVAL")
        .tone("Emotional")
        .language("EN")
        .active(true)
        .build();
  }
}
