package com.familywishes.scheduler;

import com.familywishes.dto.AiWishRequest;
import com.familywishes.dto.AiWishResponse;
import com.familywishes.entity.User;
import com.familywishes.entity.UserWishSettings;
import com.familywishes.repository.UserRepository;
import com.familywishes.repository.UserWishSettingsRepository;
import com.familywishes.service.AiService;
import com.familywishes.service.EmailService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BirthdayMailScheduler implements Job {

    private final UserWishSettingsRepository userWishSettingsRepository;
    private final UserRepository userRepository;
    private final AiService aiService;
    private final EmailService emailService;

    @Value("${alert.email.to}")
    private String alertEmail;

    @Override
    public void execute(JobExecutionContext context) {
        userWishSettingsRepository
                .findByBirthdayEnabledTrue()
                .forEach(this::triggerMessages);
    }

    private void triggerMessages(UserWishSettings event) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        List<User> users =
                userRepository.findTodaysBirthdays(
                        today.getMonthValue(),
                        today.getDayOfMonth()
                );
        log.info("Birthday users found: {}", users.size());
        for (User user : users) {
            if (today.equals(user.getLastBirthdayWishSent())) {
                log.info("Already sent birthday wish to {}", user.getEmail());
                return;
            }
            try {
                sendBirthdayWish(user);
            } catch (Exception e) {
                sendErrorEmail(user, e);
            }
        }
    }

    private void sendBirthdayWish(User user) throws JsonProcessingException {
        AiWishRequest request =
                new AiWishRequest(
                        user.getName(),
                        user.getRelationShip().name(),
                        "Birthday",
                        "",
                        "Emotional",
                        "EN"
                );
        AiWishResponse ai = aiService.generate(request);
        byte[] image = aiService.callGeminiImage(request);
        emailService.sendHtmlEmail(
                user.getEmail(),
                ai.subject(),
                ai.htmlMessage(),
                null,
                image
        );
        log.info("Birthday wish sent to {}", user.getEmail());
        user.setLastBirthdayWishSent(LocalDate.now());
        userRepository.save(user);
    }

    private void sendErrorEmail(User user, Exception e) {
        emailService.sendHtmlEmail(
                alertEmail,
                "Birthday Job Failed",
                "Failed for user: " + user.getEmail()
                        + "<br>Error: " + e.getMessage(),
                null,
                null
        );
    }
}