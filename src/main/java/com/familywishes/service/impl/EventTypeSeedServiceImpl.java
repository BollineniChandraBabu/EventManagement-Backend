package com.familywishes.service.impl;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.SeedDtos.EnumSeedRequest;
import com.familywishes.dto.SeedDtos.EnumSeedResponse;
import com.familywishes.entity.EventTypeSeed;
import com.familywishes.exception.NotFoundException;
import com.familywishes.repository.EventTypeSeedRepository;
import com.familywishes.service.EventTypeSeedService;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

  @Override
  public PagedResponse<EnumSeedResponse> list(
      int page, int size, String searchKey, String sortBy, String sortDir) {
    String normalizedSortBy = resolveSortBy(sortBy);
    Sort sort =
        "desc".equalsIgnoreCase(sortDir)
            ? Sort.by(normalizedSortBy).descending()
            : Sort.by(normalizedSortBy).ascending();
    var pageResult =
        eventTypeSeedRepository.findAllBySearchKey(
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
    EventTypeSeed eventTypeSeed = eventTypeSeedRepository.findById(id).orElse(null);
    return Objects.nonNull(eventTypeSeed) ? toResponse(eventTypeSeed) : null;
  }

  private EnumSeedResponse toResponse(EventTypeSeed seed) {
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

  @Override
  public void deleteById(Long id) {
    eventTypeSeedRepository.deleteById(id);
  }
}
