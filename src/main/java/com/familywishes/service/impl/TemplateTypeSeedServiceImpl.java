package com.familywishes.service.impl;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.SeedDtos.EnumSeedRequest;
import com.familywishes.dto.SeedDtos.EnumSeedResponse;
import com.familywishes.entity.TemplateTypeSeed;
import com.familywishes.exception.NotFoundException;
import com.familywishes.repository.TemplateTypeSeedRepository;
import com.familywishes.service.TemplateTypeSeedService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TemplateTypeSeedServiceImpl implements TemplateTypeSeedService {
  private static final String CATEGORY = "TEMPLATE_TYPE";
  private final TemplateTypeSeedRepository templateTypeSeedRepository;

  @Override
  public EnumSeedResponse create(EnumSeedRequest request) {
    String code = normalizeCode(request.code());

    TemplateTypeSeed seed =
        templateTypeSeedRepository
            .findByCode(code)
            .orElseGet(() -> TemplateTypeSeed.builder().code(code).build());
    seed.setDisplayName(request.displayName().trim());
    seed.setActive(request.active());

    return toResponse(templateTypeSeedRepository.save(seed));
  }

  @Override
  public EnumSeedResponse update(String code, EnumSeedRequest request) {
    TemplateTypeSeed seed =
        templateTypeSeedRepository
            .findByCode(normalizeCode(code))
            .orElseThrow(() -> new NotFoundException("Template type seed not found"));

    seed.setCode(normalizeCode(request.code()));
    seed.setDisplayName(request.displayName().trim());
    seed.setActive(request.active());

    return toResponse(templateTypeSeedRepository.save(seed));
  }

  @Override
  public List<EnumSeedResponse> listActive() {
    return templateTypeSeedRepository.findByActiveTrueOrderByDisplayNameAsc().stream()
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
        templateTypeSeedRepository.findAllBySearchKey(
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

  private EnumSeedResponse toResponse(TemplateTypeSeed seed) {
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
