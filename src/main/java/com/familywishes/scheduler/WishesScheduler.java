package com.familywishes.scheduler;

import com.familywishes.dto.AiWishRequest;
import com.familywishes.dto.AiWishResponse;
import com.familywishes.entity.Event;
import com.familywishes.repository.EventRepository;
import com.familywishes.service.AiService;
import com.familywishes.service.GmailEmailService;
import com.familywishes.util.WishEmailExtras;
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
    eventList.forEach(this::sendEventWish);
    emailService.retryFailed();
    log.info("Daily wish job completed");
  }

  /** One failing event must not stop the wishes of the other events. */
  private void sendEventWish(Event event) {
    try {
      AiWishRequest request =
          buildRequest(
              event.getUser().getId(),
              event.getUser().getName(),
              event.getUser().getRelationShip().getCode(),
              event.getEventType().getCode());
      AiWishResponse ai = aiService.generate(request);
      // An uploaded image is used when there is one, otherwise an image is generated.
      byte[] image = aiService.callGeminiImage(request);
      // The custom wishes page is only added for events that have one.
      String body =
          WishEmailExtras.appendWishesLink(ai.htmlMessage(), event.getWishesUrl(), linkLabel(event));
      emailService.sendEmailWithAttachments(event.getUser().getEmail(), ai.subject(), body, null, image);
    } catch (Exception e) {
      log.error("Could not send the {} wish for event {}", event.getEventType().getCode(), event.getId(), e);
    }
  }

  private static String linkLabel(Event event) {
    String name = event.getEventType().getDisplayName();
    return "Open your " + (name == null || name.isBlank() ? "event" : name.toLowerCase()) + " wishes";
  }

  private AiWishRequest buildRequest(Long userId, String name, String relation, String eventType) {
    return new AiWishRequest(name, relation, eventType, "", DEFAULT_TONE, DEFAULT_LANGUAGE, userId);
  }
}
