package com.familywishes.controller;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.FestivalDtos.FestivalWishMappingBulkRequest;
import com.familywishes.dto.FestivalDtos.FestivalWishMappingBulkResponse;
import com.familywishes.dto.FestivalDtos.FestivalWishMappingRequest;
import com.familywishes.dto.FestivalDtos.FestivalWishMappingResponse;
import com.familywishes.dto.FestivalDtos.FestivalWishMappingSummary;
import com.familywishes.service.FestivalWishMappingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/festival-wish-mappings")
@RequiredArgsConstructor
public class FestivalWishMappingController {

  private final FestivalWishMappingService festivalWishMappingService;

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public FestivalWishMappingResponse upsert(
      @Valid @RequestBody FestivalWishMappingRequest request) {
    return festivalWishMappingService.upsert(request);
  }

  @PostMapping(value = "/with-image", consumes = "multipart/form-data")
  @PreAuthorize("hasRole('ADMIN')")
  public FestivalWishMappingResponse upsertWithImage(
      @RequestParam Long specialEventId,
      @RequestParam Long userId,
      @RequestParam Boolean active,
      @RequestParam(required = false) String customMessage,
      @RequestPart(value = "image", required = false) org.springframework.web.multipart.MultipartFile image) {
    FestivalWishMappingRequest request =
        new FestivalWishMappingRequest(specialEventId, userId, active, customMessage);
    return festivalWishMappingService.upsertWithImage(request, image);
  }

  @GetMapping("/filter-values")
  @PreAuthorize("hasRole('ADMIN')")
  public java.util.Map<String, Object> getFilterValues() {
    return java.util.Map.of("statuses", java.util.List.of("ALL", "ACTIVE", "INACTIVE", "FAILED"));
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public PagedResponse<FestivalWishMappingResponse> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "") String searchKey,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) java.util.List<Long> specialEventIds,
      @RequestParam(required = false) java.util.List<Long> userIds,
      @RequestParam(required = false) java.util.List<String> statuses,
      @RequestParam(defaultValue = "id") String sortBy,
      @RequestParam(defaultValue = "desc") String sortDir) {
    if ((specialEventIds != null && !specialEventIds.isEmpty())
        || (userIds != null && !userIds.isEmpty())
        || (statuses != null && !statuses.isEmpty())) {
      return festivalWishMappingService.list(
          page, size, searchKey, specialEventIds, userIds, statuses, sortBy, sortDir);
    }
    return festivalWishMappingService.list(
        page, size, searchKey, status == null ? "ALL" : status, sortBy, sortDir);
  }

  @PostMapping("/bulk")
  @PreAuthorize("hasRole('ADMIN')")
  public FestivalWishMappingBulkResponse upsertBulk(
      @Valid @RequestBody FestivalWishMappingBulkRequest request) {
    return festivalWishMappingService.upsertBulk(request);
  }

  @GetMapping("/summary")
  @PreAuthorize("hasRole('ADMIN')")
  public FestivalWishMappingSummary summary() {
    return festivalWishMappingService.summary();
  }

  @GetMapping("/me")
  @PreAuthorize("isAuthenticated()")
  public PagedResponse<FestivalWishMappingResponse> myMappings(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "") String searchKey,
      @RequestParam(defaultValue = "id") String sortBy,
      @RequestParam(defaultValue = "desc") String sortDir) {
    return festivalWishMappingService.listForCurrentUser(page, size, searchKey, sortBy, sortDir);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(@PathVariable Long id) {
    festivalWishMappingService.delete(id);
  }
}
