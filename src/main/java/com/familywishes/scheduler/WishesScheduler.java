package com.familywishes.scheduler;

import com.familywishes.dto.AiWishRequest;
import com.familywishes.dto.AiWishResponse;
import com.familywishes.entity.Event;
import com.familywishes.repository.EventRepository;
import com.familywishes.service.AiService;
import com.familywishes.service.GmailEmailService;
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
public class WishesScheduler implements Job {
  private static final String DEFAULT_TONE = "Emotional";
  private static final String DEFAULT_LANGUAGE = "EN";

  private final EventRepository eventRepository;
  private final GmailEmailService emailService;
  private final AiService aiService;

  @Value("${scheduler.time-zone:Asia/Kolkata}")
  private String schedulerTimeZone;

  @Override
  public void execute(JobExecutionContext context) {
    LocalDate today = LocalDate.now(ZoneId.of(schedulerTimeZone));
    List<Event> eventList =
        eventRepository.findTodaysEvents(today.getMonthValue(), today.getDayOfMonth());
    eventList =
        eventList.stream()
            .filter(event -> event.isRecurring() || today.equals(event.getEventDate()))
            .toList();
    eventList.forEach(
        event -> {
          AiWishResponse ai;
          try {
            AiWishRequest request =
                buildRequest(
                    event.getUser().getName(),
                    event.getUser().getRelationShip().getCode(),
                    event.getEventType().getCode());
            ai = aiService.generate(request);
          } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
          }

          emailService.sendEmailWithAttachments(
              event.getUser().getEmail(), ai.subject(), ai.htmlMessage(), null, null);
        });
    emailService.retryFailed();
    log.info("Daily wish job completed");
  }

  private AiWishRequest buildRequest(String name, String relation, String eventType) {
    return new AiWishRequest(name, relation, eventType, "", DEFAULT_TONE, DEFAULT_LANGUAGE);
  }
}
