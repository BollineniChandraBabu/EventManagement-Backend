package com.familywishes.service.impl;

import com.familywishes.dto.SeedDtos.EnumSeedRequest;
import com.familywishes.dto.SeedDtos.EnumSeedResponse;
import com.familywishes.entity.TemplateTypeSeed;
import com.familywishes.exception.NotFoundException;
import com.familywishes.repository.TemplateTypeSeedRepository;
import com.familywishes.service.TemplateTypeSeedService;
import java.util.List;
import lombok.RequiredArgsConstructor;
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

  private EnumSeedResponse toResponse(TemplateTypeSeed seed) {
    return new EnumSeedResponse(
        seed.getId(), CATEGORY, seed.getCode(), seed.getDisplayName(), seed.isActive());
  }

  private String normalizeCode(String value) {
    return value.trim().toUpperCase();
  }
}
