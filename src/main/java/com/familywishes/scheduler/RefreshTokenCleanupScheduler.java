package com.familywishes.scheduler;

import com.familywishes.repository.RefreshTokenRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenCleanupScheduler implements Job {

  @Value("${scheduler.time-zone:Asia/Kolkata}")
  private String schedulerTimeZone;

  private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

  private final RefreshTokenRepository refreshTokenRepository;

  @Override
  public void execute(JobExecutionContext context) {
    LocalDateTime now = LocalDateTime.now(IST_ZONE);
    long deletedCount = refreshTokenRepository.deleteByExpiresAtBefore(now);
    log.info(
        "Refresh token cleanup completed. Deleted {} expired token(s) at {} IST.",
        deletedCount,
        now);
  }
}
