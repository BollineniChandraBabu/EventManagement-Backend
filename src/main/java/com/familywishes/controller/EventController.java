package com.familywishes.controller;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.EventDtos.*;
import com.familywishes.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public EventResponse update(@PathVariable Long id, @Valid @RequestBody EventRequest request) {
    return eventService.update(id, request);
  }

  @GetMapping("/{id}")
  public EventResponse getById(@PathVariable Long id, Authentication authentication) {
    boolean isAdmin =
        authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch("ROLE_ADMIN"::equals);
    return eventService.getById(id, isAdmin, authentication.getName());
  }

  @GetMapping("/filter-values")
  public java.util.Map<String, Object> getFilterValues() {
    java.util.List<Boolean> activeStatuses = java.util.List.of(true, false);
    return java.util.Map.of("active", activeStatuses);
  }

  @GetMapping
  public PagedResponse<EventResponse> list(
      Authentication authentication,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "") String searchKey,
      @RequestParam(required = false) java.util.List<String> eventTypes,
      @RequestParam(required = false) java.util.List<Long> userIds,
      @RequestParam(required = false) Boolean active,
      @RequestParam(defaultValue = "id") String sortBy,
      @RequestParam(defaultValue = "desc") String sortDir) {
    boolean isAdmin =
        authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch("ROLE_ADMIN"::equals);
    return eventService.list(
        page,
        size,
        searchKey,
        eventTypes,
        userIds,
        active,
        sortBy,
        sortDir,
        isAdmin,
        authentication.getName());
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public void deleteById(@PathVariable Long id) {
    eventService.deleteById(id);
  }
}
