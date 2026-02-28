package com.familywishes.service.impl;

import com.familywishes.dto.SeedDtos.EnumSeedRequest;
import com.familywishes.dto.SeedDtos.EnumSeedResponse;
import com.familywishes.entity.RelationshipSeed;
import com.familywishes.exception.NotFoundException;
import com.familywishes.repository.RelationshipSeedRepository;
import com.familywishes.service.RelationshipSeedService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RelationshipSeedServiceImpl implements RelationshipSeedService {
  private static final String CATEGORY = "RELATIONSHIP";
  private final RelationshipSeedRepository relationshipSeedRepository;

  @Override
  public EnumSeedResponse create(EnumSeedRequest request) {
    String code = normalizeCode(request.code());

    RelationshipSeed seed =
        relationshipSeedRepository
            .findByCode(code)
            .orElseGet(() -> RelationshipSeed.builder().code(code).build());
    seed.setDisplayName(request.displayName().trim());
    seed.setActive(request.active());

    return toResponse(relationshipSeedRepository.save(seed));
  }

  @Override
  public EnumSeedResponse update(String code, EnumSeedRequest request) {
    RelationshipSeed seed =
        relationshipSeedRepository
            .findByCode(normalizeCode(code))
            .orElseThrow(() -> new NotFoundException("Relationship seed not found"));

    seed.setCode(normalizeCode(request.code()));
    seed.setDisplayName(request.displayName().trim());
    seed.setActive(request.active());

    return toResponse(relationshipSeedRepository.save(seed));
  }

  @Override
  public List<EnumSeedResponse> listActive() {
    return relationshipSeedRepository.findByActiveTrueOrderByDisplayNameAsc().stream()
        .map(this::toResponse)
        .toList();
  }

  private EnumSeedResponse toResponse(RelationshipSeed seed) {
    return new EnumSeedResponse(
        seed.getId(), CATEGORY, seed.getCode(), seed.getDisplayName(), seed.isActive());
  }

  private String normalizeCode(String value) {
    return value.trim().toUpperCase();
  }
}
