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

  @PostMapping(value = "/with-image", consumes = "multipart/form-data")
  @PreAuthorize("hasRole('ADMIN')")
  public EventResponse createWithImage(
      @RequestParam String eventType,
      @RequestParam(required = false) java.time.LocalDate eventDate,
      @RequestParam(defaultValue = "false") boolean recurring,
      @RequestParam Long userId,
      @RequestParam(required = false) String wishesUrl,
      @RequestPart(value = "image", required = false)
          org.springframework.web.multipart.MultipartFile image) {
    EventRequest request = new EventRequest(eventType, eventDate, recurring, userId, wishesUrl);
    return eventService.create(request, image);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public EventResponse update(@PathVariable Long id, @Valid @RequestBody EventRequest request) {
    return eventService.update(id, request);
  }

  @PutMapping(value = "/{id}/with-image", consumes = "multipart/form-data")
  @PreAuthorize("hasRole('ADMIN')")
  public EventResponse updateWithImage(
      @PathVariable Long id,
      @RequestParam String eventType,
      @RequestParam(required = false) java.time.LocalDate eventDate,
      @RequestParam(defaultValue = "false") boolean recurring,
      @RequestParam Long userId,
      @RequestParam(required = false) String wishesUrl,
      @RequestPart(value = "image", required = false)
          org.springframework.web.multipart.MultipartFile image) {
    EventRequest request = new EventRequest(eventType, eventDate, recurring, userId, wishesUrl);
    return eventService.update(id, request, image);
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

  @DeleteMapping
  @PreAuthorize("hasRole('ADMIN')")
  public void deleteByIds(@RequestParam("ids") java.util.List<Long> ids) {
    eventService.deleteByIds(ids);
  }
}
