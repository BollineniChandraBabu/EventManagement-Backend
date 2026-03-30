package com.familywishes.service.impl;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.SeedDtos.EnumSeedRequest;
import com.familywishes.dto.SeedDtos.EnumSeedResponse;
import com.familywishes.entity.RelationshipSeed;
import com.familywishes.exception.NotFoundException;
import com.familywishes.repository.RelationshipSeedRepository;
import com.familywishes.service.RelationshipSeedService;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

  @Override
  public PagedResponse<EnumSeedResponse> list(
      int page, int size, String searchKey, String sortBy, String sortDir) {
    String normalizedSortBy = resolveSortBy(sortBy);
    Sort sort =
        "desc".equalsIgnoreCase(sortDir)
            ? Sort.by(normalizedSortBy).descending()
            : Sort.by(normalizedSortBy).ascending();
    var pageResult =
        relationshipSeedRepository.findAllBySearchKey(
            searchKey == null ? "" : searchKey.trim(), PageRequest.of(page, size, sort));
    return new PagedResponse<>(
        pageResult.getContent().stream().map(this::toResponse).toList(),
        pageResult.getNumber(),
        pageResult.getSize(),
        pageResult.getTotalElements(),
        pageResult.getTotalPages(),
        pageResult.hasNext(),
        pageResult.hasPrevious());
  }

  @Override
  public EnumSeedResponse getById(Long id) {
    RelationshipSeed relationshipSeed = relationshipSeedRepository.findById(id).orElse(null);
    return Objects.nonNull(relationshipSeed) ? toResponse(relationshipSeed) : null;
  }

  private EnumSeedResponse toResponse(RelationshipSeed seed) {
    return new EnumSeedResponse(
        seed.getId(), CATEGORY, seed.getCode(), seed.getDisplayName(), seed.isActive());
  }

  private String normalizeCode(String value) {
    return value.trim().toUpperCase();
  }

  private String resolveSortBy(String sortBy) {
    return switch (sortBy) {
      case "displayName", "code", "id", "createdAt", "updatedAt", "active" -> sortBy;
      default -> "displayName";
    };
  }
}
