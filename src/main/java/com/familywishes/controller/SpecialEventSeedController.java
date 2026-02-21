package com.familywishes.controller;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.SeedDtos.SpecialEventSeedRequest;
import com.familywishes.dto.SeedDtos.SpecialEventSeedResponse;
import com.familywishes.service.SpecialEventSeedService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seed/special-events")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SpecialEventSeedController {
  private final SpecialEventSeedService specialEventSeedService;

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
  public List<SpecialEventSeedResponse> listTodayActive() {
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
    return specialEventSeedService.listTodayActive(today.getDayOfMonth(), today.getMonthValue());
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
