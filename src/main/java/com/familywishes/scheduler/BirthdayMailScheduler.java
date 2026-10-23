package com.familywishes.scheduler;

import com.familywishes.dto.AiWishRequest;
import com.familywishes.dto.AiWishResponse;
import com.familywishes.entity.User;
import com.familywishes.repository.UserRepository;
import com.familywishes.service.AiService;
import com.familywishes.service.GmailEmailService;
import com.familywishes.util.WishEmailExtras;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
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
public class BirthdayMailScheduler implements Job {

  private final UserRepository userRepository;
  private final AiService aiService;
  private final GmailEmailService emailService;

  @Value("${alert.email.to}")
  private String alertEmail;

  @Value("${scheduler.time-zone:Asia/Kolkata}")
  private String schedulerTimeZone;

  @Override
  public void execute(JobExecutionContext context) {
    log.info("BIRTHDAY JOB TRIGGERED: {}", new Date());
    LocalDate today = LocalDate.now(ZoneId.of(schedulerTimeZone));
    List<User> users =
        userRepository.findTodaysBirthdays(today.getMonthValue(), today.getDayOfMonth());
    log.info("Birthday users found: {}", users.size());
    users.stream().filter(this::isBirthdayWishEnabled).forEach(user -> sendOnce(user, today));
    log.info("BIRTHDAY JOB COMPLETED: {}", new Date());
  }

  /** Each user's own "Birthday Wishes" switch decides whether they receive a wish. */
  private boolean isBirthdayWishEnabled(User user) {
    return user.getWishSettings() != null && user.getWishSettings().isBirthdayEnabled();
  }

  private void sendOnce(User user, LocalDate today) {
    if (today.equals(user.getLastBirthdayWishSent())) {
      log.info("Already sent birthday wish to {}", user.getEmail());
      return;
    }
    try {
      sendBirthdayWish(user, today);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      sendErrorEmail(user, e);
    }
  }

  private void sendBirthdayWish(User user, LocalDate today) throws JsonProcessingException {
    AiWishRequest request =
        new AiWishRequest(
            user.getName(),
            user.getRelationShip().getCode(),
            "Birthday",
            "",
            "Emotional",
            "EN",
            user.getId());
    AiWishResponse ai = aiService.generate(request);
    // An uploaded image is used when there is one, otherwise an image is generated.
    byte[] image = aiService.callGeminiImage(request);
    // The custom wishes page is only added for users who have one.
    String body =
        WishEmailExtras.appendWishesLink(
            ai.htmlMessage(), user.getBirthdayWishesUrl(), "Open your birthday wishes");
    emailService.sendEmailWithAttachments(user.getEmail(), ai.subject(), body, null, image);
    log.info("Birthday wish sent to {}", user.getEmail());
    user.setLastBirthdayWishSent(today);
    userRepository.save(user);
  }

  private void sendErrorEmail(User user, Exception e) {
    emailService.sendEmailWithAttachments(
        alertEmail,
        "Birthday Job Failed",
        "Failed for user: " + user.getEmail() + "<br>Error: " + e.getMessage(),
        null,
        null);
  }
}
