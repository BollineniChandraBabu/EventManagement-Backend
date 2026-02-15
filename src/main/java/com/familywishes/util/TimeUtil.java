package com.familywishes.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class TimeUtil {

    public boolean canSend(LocalDateTime lastUserMessageTime) {
        if (lastUserMessageTime == null) return false;

        return lastUserMessageTime.isAfter(
                LocalDateTime.now().minusHours(24)
        );
    }
}