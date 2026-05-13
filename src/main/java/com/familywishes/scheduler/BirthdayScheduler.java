package com.familywishes.scheduler;

import com.familywishes.entity.InstagramUser;
import com.familywishes.entity.MessageLog;
import com.familywishes.entity.MessageStatus;
import com.familywishes.entity.MessageType;
import com.familywishes.repository.IGMessageLogRepository;
import com.familywishes.repository.InstagramUserRepository;
import com.familywishes.service.SchedulerTrackingService;
import com.familywishes.service.impl.MessageDispatcher;
import com.familywishes.util.TimeUtil;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BirthdayScheduler {

  private final InstagramUserRepository userRepo;
  private final IGMessageLogRepository logRepo;
  private final MessageDispatcher dispatcher;
  private final TimeUtil timeUtil;
  private final SchedulerTrackingService schedulerTrackingService;

  @Value("${scheduler.time-zone:Asia/Kolkata}")
  private String schedulerTimeZone;

  @Scheduled(cron = "0 0 7 * * ?", zone = "${scheduler.time-zone}")
  public void sendBirthdayWishes() {
    schedulerTrackingService.track(
        "instagramBirthdayScheduler",
        () -> {
          LocalDate today = LocalDate.now(ZoneId.of(schedulerTimeZone));

          List<InstagramUser> users =
              userRepo.findTodaysBirthdays(today.getMonthValue(), today.getDayOfMonth());

          for (InstagramUser user : users) {

            if (timeUtil.canSend(user.getLastUserMessageTime())) {

              MessageLog log = new MessageLog();
              log.setInstagramUserId(user.getInstagramUserId());
              log.setMessage("🎂 Happy Birthday " + user.getName());
              log.setStatus(MessageStatus.PENDING);
              log.setCreatedAt(LocalDateTime.now(ZoneId.of(schedulerTimeZone)));
              log.setMessageType(MessageType.BIRTHDAY);
              logRepo.save(log);
              dispatcher.sendAsync(log);
            }
          }
        });
  }
}
