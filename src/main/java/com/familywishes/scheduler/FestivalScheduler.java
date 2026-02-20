package com.familywishes.scheduler;

import com.familywishes.entity.InstagramUser;
import com.familywishes.entity.MessageLog;
import com.familywishes.entity.MessageStatus;
import com.familywishes.entity.SpecialEvent;
import com.familywishes.repository.IGMessageLogRepository;
import com.familywishes.repository.InstagramUserRepository;
import com.familywishes.repository.SpecialEventRepository;
import com.familywishes.service.impl.MessageDispatcher;
import com.familywishes.util.TimeUtil;
import com.familywishes.service.SchedulerTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FestivalScheduler {

    private final SpecialEventRepository eventRepo;
    private final InstagramUserRepository userRepo;
    private final IGMessageLogRepository logRepo;
    private final MessageDispatcher dispatcher;
    private final TimeUtil timeUtil;
    private final SchedulerTrackingService schedulerTrackingService;

    @Scheduled(cron = "0 30 7 * * ?", zone = "${scheduler.time-zone}")
    public void sendFestivalWishes() {
        schedulerTrackingService.track("instagramFestivalScheduler", () -> {

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));

        List<SpecialEvent> events =
                eventRepo.findByDayAndMonthAndActiveTrue(
                        today.getDayOfMonth(),
                        today.getMonthValue()
                );

        if (events.isEmpty()) return;


        List<InstagramUser> users = userRepo.findAll();

        for (SpecialEvent event : events) {
            for (InstagramUser user : users) {

                if (timeUtil.canSend(user.getLastUserMessageTime())) {

                    MessageLog log = new MessageLog();
                    log.setInstagramUserId(user.getInstagramUserId());
                    log.setMessage(event.getMessage());
                    log.setStatus(MessageStatus.PENDING);
                    log.setCreatedAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));

                    logRepo.save(log);
                    dispatcher.sendAsync(log);
                }
            }
        }
        });
    }
}
