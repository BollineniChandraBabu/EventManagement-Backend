package com.familywishes.controller;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.SeedDtos.EnumSeedRequest;
import com.familywishes.dto.SeedDtos.EnumSeedResponse;
import com.familywishes.service.TemplateTypeSeedService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seed/template-types")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class TemplateTypeSeedController {
  private final TemplateTypeSeedService templateTypeSeedService;

  @PostMapping
  public EnumSeedResponse create(@Valid @RequestBody EnumSeedRequest request) {
    return templateTypeSeedService.create(request);
  }

  @PutMapping("/{code}")
  public EnumSeedResponse update(
      @PathVariable String code, @Valid @RequestBody EnumSeedRequest request) {
    return templateTypeSeedService.update(code, request);
  }

  @GetMapping
  public PagedResponse<EnumSeedResponse> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "") String searchKey,
      @RequestParam(defaultValue = "displayName") String sortBy,
      @RequestParam(defaultValue = "asc") String sortDir) {
    return templateTypeSeedService.list(page, size, searchKey, sortBy, sortDir);
  }
}
