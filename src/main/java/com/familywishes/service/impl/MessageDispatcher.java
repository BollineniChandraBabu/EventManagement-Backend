package com.familywishes.service.impl;

import com.familywishes.entity.MessageLog;
import com.familywishes.entity.MessageStatus;
import com.familywishes.repository.IGMessageLogRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MessageDispatcher {

    private final InstagramService instagramService;
    private final IGMessageLogRepository repo;

    public MessageDispatcher(InstagramService instagramService,
                             IGMessageLogRepository repo) {
        this.instagramService = instagramService;
        this.repo = repo;
    }

    @Async
    public void sendAsync(MessageLog log) {

        try {
            log.setStatus(MessageStatus.RETRYING);
            log.setLastAttemptTime(LocalDateTime.now());
            repo.save(log);

            instagramService.sendMessage(
                    log
            );

            log.setStatus(MessageStatus.SENT);

        } catch (Exception e) {
            log.setStatus(MessageStatus.FAILED);
            log.setRetryCount(log.getRetryCount() + 1);
            log.setErrorMessage(e.getMessage());
        }

        repo.save(log);
    }
}