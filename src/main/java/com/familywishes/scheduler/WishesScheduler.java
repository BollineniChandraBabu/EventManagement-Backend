package com.familywishes.scheduler;

import com.familywishes.dto.AiWishRequest;
import com.familywishes.dto.AiWishResponse;
import com.familywishes.repository.EmailTemplateRepository;
import com.familywishes.repository.EventRepository;
import com.familywishes.repository.UserWishSettingsRepository;
import com.familywishes.service.AiService;
import com.familywishes.service.BrevoEmailService;
import com.familywishes.util.TemplateRenderer;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WishesScheduler implements Job {
        private final EventRepository eventRepository;
        private final UserWishSettingsRepository userWishSettingsRepository;
        private final EmailTemplateRepository templateRepository;
        private final BrevoEmailService emailService;
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
                    AiWishResponse ai = null;
                    try {
                        ai = aiService.generate(new AiWishRequest(event.getUser().getName(), "Family", event.getEventType().name(), event.getFestivalName(), "Emotional", "EN"));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                    subject = ai.subject();
                    html = ai.htmlMessage();
                }
                emailService.sendEmailWithAttachments(event.getUser().getEmail(), subject, html, null, null);
            });
            emailService.retryFailed();
            log.info("Daily wish job completed");
        }

}
