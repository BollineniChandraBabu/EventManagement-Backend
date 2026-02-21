package com.familywishes.service.impl;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.SeedDtos.SpecialEventSeedRequest;
import com.familywishes.dto.SeedDtos.SpecialEventSeedResponse;
import com.familywishes.dto.SeedDtos.WishTemplateSeedRequest;
import com.familywishes.dto.SeedDtos.WishTemplateSeedResponse;
import com.familywishes.entity.SpecialEvent;
import com.familywishes.entity.WishSeedTemplate;
import com.familywishes.entity.enums.SeedTemplateType;
import com.familywishes.exception.NotFoundException;
import com.familywishes.repository.SpecialEventRepository;
import com.familywishes.repository.WishSeedTemplateRepository;
import com.familywishes.service.SeedService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SeedServiceImpl implements SeedService {
  private final SpecialEventRepository specialEventRepository;
  private final WishSeedTemplateRepository wishSeedTemplateRepository;

  @Override
  public SpecialEventSeedResponse createSpecialEventSeed(SpecialEventSeedRequest request) {
    SpecialEvent event =
        SpecialEvent.builder()
            .eventName(request.eventName())
            .day(request.day())
            .month(request.month())
            .message(request.message())
            .active(request.active())
            .build();
    return toResponse(specialEventRepository.save(event));
  }

  @Override
  public SpecialEventSeedResponse getSpecialEventSeedById(Long id) {
    return toResponse(
        specialEventRepository
            .findById(id)
            .orElseThrow(() -> new NotFoundException("Special event seed not found")));
  }

  @Override
  public PagedResponse<SpecialEventSeedResponse> listSpecialEventSeeds(
      int page, int size, String searchKey, String sortBy, String sortDir) {
    Sort.Direction direction =
        "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
    String normalizedSortBy = (sortBy == null || sortBy.isBlank()) ? "id" : sortBy.trim();

    Page<SpecialEvent> specialEventPage =
        specialEventRepository.findAllBySearchKey(
            searchKey == null ? "" : searchKey.trim(),
            PageRequest.of(page, size, Sort.by(direction, normalizedSortBy)));

    return new PagedResponse<>(
        specialEventPage.getContent().stream().map(this::toResponse).toList(),
        specialEventPage.getNumber(),
        specialEventPage.getSize(),
        specialEventPage.getTotalElements(),
        specialEventPage.getTotalPages(),
        specialEventPage.hasNext(),
        specialEventPage.hasPrevious());
  }

  @Override
  public List<SpecialEventSeedResponse> listTodayActiveSpecialEventSeeds(int day, int month) {
    return specialEventRepository.findByDayAndMonthAndActiveTrue(day, month).stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  public SpecialEventSeedResponse updateSpecialEventSeed(Long id, SpecialEventSeedRequest request) {
    SpecialEvent event =
        specialEventRepository
            .findById(id)
            .orElseThrow(() -> new NotFoundException("Special event seed not found"));

    event.setEventName(request.eventName());
    event.setDay(request.day());
    event.setMonth(request.month());
    event.setMessage(request.message());
    event.setActive(request.active());

    return toResponse(specialEventRepository.save(event));
  }

  @Override
  public void deleteSpecialEventSeed(Long id) {
    if (!specialEventRepository.existsById(id)) {
      throw new NotFoundException("Special event seed not found");
    }
    specialEventRepository.deleteById(id);
  }

  @Override
  public WishTemplateSeedResponse createWishTemplateSeed(WishTemplateSeedRequest request) {
    WishSeedTemplate seed =
        wishSeedTemplateRepository
            .findByType(request.type())
            .orElseGet(() -> WishSeedTemplate.builder().type(request.type()).build());

    seed.setRelation(request.relation());
    seed.setEvent(request.event());
    seed.setTone(request.tone());
    seed.setLanguage(request.language());
    seed.setActive(request.active());

    return toWishTemplateResponse(wishSeedTemplateRepository.save(seed));
  }

  @Override
  public WishTemplateSeedResponse getWishTemplateSeedByType(SeedTemplateType type) {
    return toWishTemplateResponse(
        wishSeedTemplateRepository
            .findByType(type)
            .orElseThrow(() -> new NotFoundException("Wish template seed not found")));
  }

  @Override
  public List<WishTemplateSeedResponse> listWishTemplateSeeds() {
    return wishSeedTemplateRepository.findAll().stream().map(this::toWishTemplateResponse).toList();
  }

  @Override
  public WishTemplateSeedResponse updateWishTemplateSeed(
      SeedTemplateType type, WishTemplateSeedRequest request) {
    WishSeedTemplate seed =
        wishSeedTemplateRepository
            .findByType(type)
            .orElseThrow(() -> new NotFoundException("Wish template seed not found"));

    seed.setRelation(request.relation());
    seed.setEvent(request.event());
    seed.setTone(request.tone());
    seed.setLanguage(request.language());
    seed.setActive(request.active());

    return toWishTemplateResponse(wishSeedTemplateRepository.save(seed));
  }

  private SpecialEventSeedResponse toResponse(SpecialEvent event) {
    return new SpecialEventSeedResponse(
        event.getId(),
        event.getEventName(),
        event.getDay(),
        event.getMonth(),
        event.getMessage(),
        event.isActive());
  }

  private WishTemplateSeedResponse toWishTemplateResponse(WishSeedTemplate seed) {
    return new WishTemplateSeedResponse(
        seed.getId(),
        seed.getType(),
        seed.getRelation(),
        seed.getEvent(),
        seed.getTone(),
        seed.getLanguage(),
        seed.isActive());
  }
}
