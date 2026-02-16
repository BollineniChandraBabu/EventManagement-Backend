package com.familywishes.dto;

import com.familywishes.entity.enums.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class EventDtos {
    public record EventRequest(@NotBlank String subject, @NotBlank String body, @NotNull EventType eventType, String festivalName, LocalDate eventDate, boolean recurring, @NotNull Long userId) {}
    public record EventResponse(Long id, String subject, String body, EventType eventType, String festivalName, LocalDate eventDate, boolean recurring, Long userId, boolean active) {}
}
