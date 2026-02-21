package com.familywishes.scheduler;

import com.familywishes.dto.AiWishRequest;
import com.familywishes.dto.AiWishResponse;
import com.familywishes.repository.EventRepository;
import com.familywishes.service.AiService;
import com.familywishes.service.GmailEmailService;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
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
  private final GmailEmailService emailService;
  private final AiService aiService;

  @Override
  public void execute(JobExecutionContext context) {
    eventRepository
        .findByEventDateAndActiveTrue(LocalDate.now(ZoneId.of("Asia/Kolkata")))
        .forEach(
            event -> {
              var vars =
                  Map.of(
                      "name",
                      event.getUser().getName(),
                      "relation",
                      "Family",
                      "eventDate",
                      event.getEventDate().toString(),
                      "festival",
                      event.getFestivalName() == null ? "" : event.getFestivalName());
              String subject;
              String html;
              AiWishResponse ai = null;
              try {
                ai =
                    aiService.generate(
                        new AiWishRequest(
                            event.getUser().getName(),
                            "Family",
                            event.getEventType().name(),
                            event.getFestivalName(),
                            "Emotional",
                            "EN"));
              } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
              }
              subject = ai.subject();
              html = ai.htmlMessage();
              emailService.sendEmailWithAttachments(
                  event.getUser().getEmail(), subject, html, null, null);
            });
    emailService.retryFailed();
    log.info("Daily wish job completed");
  }
}
