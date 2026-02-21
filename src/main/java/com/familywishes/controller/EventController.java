package com.familywishes.controller;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.EventDtos.*;
import com.familywishes.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {
  private final EventService eventService;

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public EventResponse create(@Valid @RequestBody EventRequest request) {
    return eventService.create(request);
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public PagedResponse<EventResponse> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "") String searchKey,
      @RequestParam(defaultValue = "id") String sortBy,
      @RequestParam(defaultValue = "desc") String sortDir) {
    return eventService.list(page, size, searchKey, sortBy, sortDir);
  }
}
