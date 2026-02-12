package com.familywishes.controller;

import com.familywishes.dto.EventDtos.*;
import com.familywishes.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    @PostMapping
    public EventResponse create(@Valid @RequestBody EventRequest request) { return eventService.create(request); }

    @GetMapping
    public List<EventResponse> list() { return eventService.list(); }
}
