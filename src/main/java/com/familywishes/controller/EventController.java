package com.familywishes.controller;

import com.familywishes.dto.EventDtos.*;
import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    @PostMapping
    public EventResponse create(@Valid @RequestBody EventRequest request) { return eventService.create(request); }

    @GetMapping
    public PagedResponse<EventResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String searchKey
    ) { return eventService.list(page, size, searchKey); }
}
