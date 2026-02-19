package com.familywishes.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class TimeUtil {

    public boolean canSend(LocalDateTime lastUserMessageTime) {
        if (lastUserMessageTime == null) return false;

        return lastUserMessageTime.isAfter(
                LocalDateTime.now(ZoneId.of("Asia/Kolkata")).minusHours(24)
        );
    }

    public static LocalDateTime toIST(LocalDateTime utcTime) {
        if (utcTime == null) return null;

        return utcTime
                .atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(ZoneId.of("Asia/Kolkata"))
                .toLocalDateTime();
    }
}