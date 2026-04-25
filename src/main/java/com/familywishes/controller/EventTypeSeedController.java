package com.familywishes.controller;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.SeedDtos.EnumSeedRequest;
import com.familywishes.dto.SeedDtos.EnumSeedResponse;
import com.familywishes.service.EventTypeSeedService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seed/event-types")
@RequiredArgsConstructor
public class EventTypeSeedController {
  private final EventTypeSeedService eventTypeSeedService;

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public EnumSeedResponse create(@Valid @RequestBody EnumSeedRequest request) {
    return eventTypeSeedService.create(request);
  }

  @PutMapping("/{code}")
  @PreAuthorize("hasRole('ADMIN')")
  public EnumSeedResponse update(
      @PathVariable String code, @Valid @RequestBody EnumSeedRequest request) {
    return eventTypeSeedService.update(code, request);
  }

  @GetMapping
  public PagedResponse<EnumSeedResponse> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "") String searchKey,
      @RequestParam(defaultValue = "displayName") String sortBy,
      @RequestParam(defaultValue = "asc") String sortDir) {
    return eventTypeSeedService.list(page, size, searchKey, sortBy, sortDir);
  }

  @GetMapping("/{id}")
  public EnumSeedResponse getById(@PathVariable Long id) {
    return eventTypeSeedService.getById(id);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public void deleteById(@PathVariable Long id) {
    eventTypeSeedService.deleteById(id);
  }
}
