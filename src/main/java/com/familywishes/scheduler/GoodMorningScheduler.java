package com.familywishes.scheduler;

import com.familywishes.dto.AiWishRequest;
import com.familywishes.dto.AiWishResponse;
import com.familywishes.entity.UserWishSettings;
import com.familywishes.repository.UserWishSettingsRepository;
import com.familywishes.service.AiService;
import com.familywishes.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class GoodMorningScheduler implements Job {

    private final UserWishSettingsRepository userWishSettingsRepository;
    private final AiService aiService;
    private final EmailService emailService;

    @Value("${alert.email.to}")
    private String alertEmail;

    @Override
    public void execute(JobExecutionContext context) {
        System.out.println("GOOD MORNING JOB TRIGGERED: " + new Date());
        log.info("GOOD MORNING JOB TRIGGERED: " + new Date());
        userWishSettingsRepository
                .findByGoodMorningEnabledTrue()
                .forEach(this::triggerMessages);
        System.out.println("GOOD MORNING JOB COMPLETED: " + new Date());
        log.info("GOOD MORNING JOB COMPLETED: " + new Date());
    }

    private void triggerMessages(UserWishSettings event) {

        try {

            AiWishRequest request =
                    new AiWishRequest(
                            event.getUser().getName(),
                            event.getUser().getRelationShip().name(),
                            "Good Morning",
                            "",
                            "Emotional",
                            "EN"
                    );

            AiWishResponse ai = aiService.generate(request);

            byte[] image = aiService.callGeminiImage(request);

            emailService.sendHtmlEmail(
                    event.getUser().getEmail(),
                    ai.subject(),
                    ai.htmlMessage(),
                    null,
                    image
            );

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            sendErrorEmail(e);
        }
    }

    private void sendErrorEmail(Exception e) {

        String subject = "🚨 Family Wishes Scheduler Failed";

        String body =
                "<h3>Error in Good Morning Job</h3>" +
                        "<p>" + e.getMessage() + "</p>";

        emailService.sendHtmlEmail(
                alertEmail,
                subject,
                body,
                null,
                null
        );

    }

}