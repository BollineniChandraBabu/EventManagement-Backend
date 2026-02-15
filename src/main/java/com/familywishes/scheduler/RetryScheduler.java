package com.familywishes.scheduler;

import com.familywishes.entity.MessageLog;
import com.familywishes.entity.MessageStatus;
import com.familywishes.repository.IGMessageLogRepository;
import com.familywishes.service.impl.MessageDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RetryScheduler {

    private final IGMessageLogRepository repo;
    private final MessageDispatcher dispatcher;

    @Scheduled(cron = "0 */15 * * * ?", zone = "${scheduler.time-zone}")
    public void retryFailed() {

        List<MessageLog> failed =
                repo.findByStatusAndRetryCountLessThan(
                        MessageStatus.FAILED,
                        3
                );

        for (MessageLog log : failed) {
            dispatcher.sendAsync(log);
        }
    }
}
