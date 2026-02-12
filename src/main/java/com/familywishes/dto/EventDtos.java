package com.familywishes.dto;

import com.familywishes.entity.enums.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class EventDtos {
    public record EventRequest(@NotBlank String title, @NotNull EventType eventType, String festivalName, @NotNull LocalDate eventDate, boolean recurring, @NotNull Long userId) {}
    public record EventResponse(Long id, String title, EventType eventType, String festivalName, LocalDate eventDate, boolean recurring, Long userId, boolean active) {}
}
