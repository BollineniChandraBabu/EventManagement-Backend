package com.familywishes.controller;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.SeedDtos.EnumSeedRequest;
import com.familywishes.dto.SeedDtos.EnumSeedResponse;
import com.familywishes.service.RelationshipSeedService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seed/relationships")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RelationshipSeedController {
  private final RelationshipSeedService relationshipSeedService;

  @PostMapping
  public EnumSeedResponse create(@Valid @RequestBody EnumSeedRequest request) {
    return relationshipSeedService.create(request);
  }

  @PutMapping("/{code}")
  public EnumSeedResponse update(
      @PathVariable String code, @Valid @RequestBody EnumSeedRequest request) {
    return relationshipSeedService.update(code, request);
  }

  @GetMapping("/filter-values")
  public java.util.Map<String, Object> getFilterValues() {
    java.util.List<String> codes = relationshipSeedService.listActive().stream().map(EnumSeedResponse::code).toList();
    java.util.List<Boolean> activeStatuses = java.util.List.of(true, false);
    return java.util.Map.of("codes", codes, "active", activeStatuses);
  }

  @GetMapping
  public PagedResponse<EnumSeedResponse> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "") String searchKey,
      @RequestParam(required = false) java.util.List<String> codes,
      @RequestParam(required = false) Boolean active,
      @RequestParam(defaultValue = "displayName") String sortBy,
      @RequestParam(defaultValue = "asc") String sortDir) {
    return relationshipSeedService.list(page, size, searchKey, codes, active, sortBy, sortDir);
  }

  @GetMapping("/{id}")
  public EnumSeedResponse getById(@PathVariable Long id) {
    return relationshipSeedService.getById(id);
  }

  @DeleteMapping("/{id}")
  public void deleteById(@PathVariable Long id) {
    relationshipSeedService.deleteById(id);
  }
}
