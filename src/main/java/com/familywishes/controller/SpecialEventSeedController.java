package com.familywishes.controller;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.SeedDtos.SpecialEventSeedRequest;
import com.familywishes.dto.SeedDtos.SpecialEventSeedResponse;
import com.familywishes.service.SpecialEventSeedService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seed/special-events")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SpecialEventSeedController {
  private final SpecialEventSeedService specialEventSeedService;

  @Value("${scheduler.time-zone:Asia/Kolkata}")
  private String schedulerTimeZone;

  @PostMapping
  public SpecialEventSeedResponse create(@Valid @RequestBody SpecialEventSeedRequest request) {
    return specialEventSeedService.create(request);
  }

  @GetMapping("/{id}")
  public SpecialEventSeedResponse getById(@PathVariable Long id) {
    return specialEventSeedService.getById(id);
  }

  @GetMapping
  public PagedResponse<SpecialEventSeedResponse> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "") String searchKey,
      @RequestParam(defaultValue = "id") String sortBy,
      @RequestParam(defaultValue = "desc") String sortDir) {
    return specialEventSeedService.list(page, size, searchKey, sortBy, sortDir);
  }

  @GetMapping("/today")
  public PagedResponse<SpecialEventSeedResponse> listTodayActive(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "") String searchKey,
      @RequestParam(defaultValue = "eventName") String sortBy,
      @RequestParam(defaultValue = "asc") String sortDir) {
    LocalDate today = LocalDate.now(ZoneId.of(schedulerTimeZone));
    return specialEventSeedService.listTodayActive(today, page, size, searchKey, sortBy, sortDir);
  }

  @PutMapping("/{id}")
  public SpecialEventSeedResponse update(
      @PathVariable Long id, @Valid @RequestBody SpecialEventSeedRequest request) {
    return specialEventSeedService.update(id, request);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    specialEventSeedService.delete(id);
  }
}
