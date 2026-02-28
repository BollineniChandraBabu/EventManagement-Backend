package com.familywishes.controller;

import com.familywishes.dto.SeedDtos.EnumSeedRequest;
import com.familywishes.dto.SeedDtos.EnumSeedResponse;
import com.familywishes.service.RelationshipSeedService;
import jakarta.validation.Valid;
import java.util.List;
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

  @GetMapping
  public List<EnumSeedResponse> listActive() {
    return relationshipSeedService.listActive();
  }

  @GetMapping("/{id}")
  public EnumSeedResponse getById(@PathVariable Long id) {
    return relationshipSeedService.getById(id);
  }
}
