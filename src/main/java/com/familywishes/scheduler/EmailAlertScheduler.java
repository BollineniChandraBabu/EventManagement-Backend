package com.familywishes.scheduler;

import com.familywishes.entity.MessageStatus;
import com.familywishes.repository.IGMessageLogRepository;
import com.familywishes.service.EmailService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class EmailAlertScheduler {

    private final IGMessageLogRepository repo;
    private final EmailService emailService;

    public EmailAlertScheduler(IGMessageLogRepository repo, EmailService emailService) {
        this.repo = repo;
        this.emailService = emailService;
    }

    @Scheduled(cron = "0 */30 * * * ?", zone = "${scheduler.time-zone}")
    public void checkFailures() {

        LocalDateTime start =
                LocalDate.now().atStartOfDay();

        long failed =
                repo.countByStatusAndCreatedAtBetween(
                        MessageStatus.FAILED,
                        start,
                        LocalDateTime.now()
                );

        if (failed > 5) {
            emailService.sendFailureAlert(failed);
        }
    }
}