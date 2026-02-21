package com.familywishes.scheduler;

import com.familywishes.dto.AiWishRequest;
import com.familywishes.dto.AiWishResponse;
import com.familywishes.entity.UserWishSettings;
import com.familywishes.entity.WishSeedTemplate;
import com.familywishes.repository.UserWishSettingsRepository;
import com.familywishes.repository.WishSeedTemplateRepository;
import com.familywishes.service.AiService;
import com.familywishes.service.GmailEmailService;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GoodMorningScheduler implements Job {

  private final UserWishSettingsRepository userWishSettingsRepository;
  private final WishSeedTemplateRepository wishSeedTemplateRepository;
  private final AiService aiService;
  private final GmailEmailService emailService;

  @Value("${alert.email.to}")
  private String alertEmail;

  @Override
  public void execute(JobExecutionContext context) {
    System.out.println("GOOD MORNING JOB TRIGGERED: " + new Date());
    log.info("GOOD MORNING JOB TRIGGERED: " + new Date());
    userWishSettingsRepository.findByGoodMorningEnabledTrue().forEach(this::triggerMessages);
    System.out.println("GOOD MORNING JOB COMPLETED: " + new Date());
    log.info("GOOD MORNING JOB COMPLETED: " + new Date());
  }

  private void triggerMessages(UserWishSettings event) {
    try {
      WishSeedTemplate template =
          wishSeedTemplateRepository
              .findByType_CodeAndActiveTrue("GOOD_MORNING")
              .orElse(defaultGoodMorningTemplate());

      AiWishRequest request =
          new AiWishRequest(
              event.getUser().getName(),
              template.getRelation(),
              template.getEvent(),
              "",
              template.getTone(),
              template.getLanguage());

      AiWishResponse ai = aiService.generate(request);
      byte[] image = aiService.callGeminiImage(request);

      emailService.sendEmailWithAttachments(
          event.getUser().getEmail(), ai.subject(), ai.htmlMessage(), null, image);

    } catch (Exception e) {
      log.error(e.getMessage(), e);
      sendErrorEmail(e);
    }
  }

  private WishSeedTemplate defaultGoodMorningTemplate() {
    return WishSeedTemplate.builder()
        .type(null)
        .relation("Family")
        .event("Good Morning")
        .tone("Emotional")
        .language("EN")
        .active(true)
        .build();
  }

  private void sendErrorEmail(Exception e) {
    String subject = "🚨 Family Wishes Scheduler Failed";
    String body = "<h3>Error in Good Morning Job</h3>" + "<p>" + e.getMessage() + "</p>";

    emailService.sendEmailWithAttachments(alertEmail, subject, body, null, null);
  }
}
