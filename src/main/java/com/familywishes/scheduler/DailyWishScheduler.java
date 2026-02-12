package com.familywishes.scheduler;

import com.familywishes.dto.AiWishRequest;
import com.familywishes.repository.EmailTemplateRepository;
import com.familywishes.repository.EventRepository;
import com.familywishes.service.AiService;
import com.familywishes.service.EmailService;
import com.familywishes.util.TemplateRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

@Configuration
public class DailyWishScheduler {
    @Bean
    JobDetail wishesJobDetail() {
        return JobBuilder.newJob(WishesJob.class).withIdentity("dailyWishJob").storeDurably().build();
    }

    @Bean
    Trigger wishesTrigger(JobDetail wishesJobDetail) {
        return TriggerBuilder.newTrigger().forJob(wishesJobDetail).withIdentity("dailyWishTrigger")
                .withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(8, 0)).build();
    }

    @Component
    @RequiredArgsConstructor
    @Slf4j
    public static class WishesJob implements Job {
        private final EventRepository eventRepository;
        private final EmailTemplateRepository templateRepository;
        private final EmailService emailService;
        private final AiService aiService;
        private final TemplateRenderer renderer;

        @Override
        public void execute(JobExecutionContext context) {
            eventRepository.findByEventDateAndActiveTrue(LocalDate.now()).forEach(event -> {
                var vars = Map.of("name", event.getUser().getName(), "relation", "Family", "eventDate", event.getEventDate().toString(),
                        "festival", event.getFestivalName() == null ? "" : event.getFestivalName());
                String subject;
                String html;
                if (event.getEventType().name().equals("FESTIVAL")) {
                    var tpl = templateRepository.findByNameOrderByVersionDesc(event.getFestivalName()).stream().findFirst().orElse(null);
                    subject = tpl != null ? renderer.render(tpl.getSubject(), vars) : "Happy " + event.getFestivalName();
                    html = tpl != null ? renderer.render(tpl.getHtmlContent(), vars) : "<p>Best wishes</p>";
                } else {
                    var ai = aiService.generate(new AiWishRequest(event.getUser().getName(), "Family", event.getEventType().name(), event.getFestivalName(), "Emotional", "EN"));
                    subject = ai.subject();
                    html = ai.htmlMessage();
                }
                emailService.sendHtmlEmail(event.getUser().getEmail(), subject, html, null);
            });
            emailService.retryFailed();
            log.info("Daily wish job completed");
        }
    }
}
