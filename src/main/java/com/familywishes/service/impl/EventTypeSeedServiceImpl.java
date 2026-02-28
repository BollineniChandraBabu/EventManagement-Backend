package com.familywishes.service.impl;

import com.familywishes.dto.SeedDtos.EnumSeedRequest;
import com.familywishes.dto.SeedDtos.EnumSeedResponse;
import com.familywishes.entity.EventTypeSeed;
import com.familywishes.exception.NotFoundException;
import com.familywishes.repository.EventTypeSeedRepository;
import com.familywishes.service.EventTypeSeedService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventTypeSeedServiceImpl implements EventTypeSeedService {
  private static final String CATEGORY = "EVENT_TYPE";
  private final EventTypeSeedRepository eventTypeSeedRepository;

  @Override
  public EnumSeedResponse create(EnumSeedRequest request) {
    String code = normalizeCode(request.code());

    EventTypeSeed seed =
        eventTypeSeedRepository
            .findByCode(code)
            .orElseGet(() -> EventTypeSeed.builder().code(code).build());
    seed.setDisplayName(request.displayName().trim());
    seed.setActive(request.active());

    return toResponse(eventTypeSeedRepository.save(seed));
  }

  @Override
  public EnumSeedResponse update(String code, EnumSeedRequest request) {
    EventTypeSeed seed =
        eventTypeSeedRepository
            .findByCode(normalizeCode(code))
            .orElseThrow(() -> new NotFoundException("Event type seed not found"));

    seed.setCode(normalizeCode(request.code()));
    seed.setDisplayName(request.displayName().trim());
    seed.setActive(request.active());

    return toResponse(eventTypeSeedRepository.save(seed));
  }

  @Override
  public List<EnumSeedResponse> listActive() {
    return eventTypeSeedRepository.findByActiveTrueOrderByDisplayNameAsc().stream()
        .map(this::toResponse)
        .toList();
  }

  private EnumSeedResponse toResponse(EventTypeSeed seed) {
    return new EnumSeedResponse(
        seed.getId(), CATEGORY, seed.getCode(), seed.getDisplayName(), seed.isActive());
  }

  private String normalizeCode(String value) {
    return value.trim().toUpperCase();
  }
}
