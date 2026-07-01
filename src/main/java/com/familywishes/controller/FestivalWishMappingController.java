package com.familywishes.controller;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.FestivalDtos.FestivalWishMappingRequest;
import com.familywishes.dto.FestivalDtos.FestivalWishMappingResponse;
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

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public PagedResponse<FestivalWishMappingResponse> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "") String searchKey,
      @RequestParam(defaultValue = "id") String sortBy,
      @RequestParam(defaultValue = "desc") String sortDir) {
    return festivalWishMappingService.list(page, size, searchKey, sortBy, sortDir);
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
