package com.familywishes.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TimeUtil {

  @Value("${scheduler.time-zone:Asia/Kolkata}")
  private String schedulerTimeZone;

  public boolean canSend(LocalDateTime lastUserMessageTime) {
    if (lastUserMessageTime == null) return false;

    return lastUserMessageTime.isAfter(
        LocalDateTime.now(ZoneId.of(schedulerTimeZone)).minusHours(24));
  }

  public static LocalDateTime toIST(LocalDateTime utcTime) {
    if (utcTime == null) return null;

    return utcTime
        .atZone(ZoneId.of("UTC"))
        .withZoneSameInstant(ZoneId.of("Asia/Kolkata"))
        .toLocalDateTime();
  }
}
