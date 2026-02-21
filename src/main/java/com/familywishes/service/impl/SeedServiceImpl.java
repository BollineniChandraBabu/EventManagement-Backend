package com.familywishes.service.impl;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.SeedDtos.EnumSeedRequest;
import com.familywishes.dto.SeedDtos.EnumSeedResponse;
import com.familywishes.dto.SeedDtos.SpecialEventSeedRequest;
import com.familywishes.dto.SeedDtos.SpecialEventSeedResponse;
import com.familywishes.entity.EventTypeSeed;
import com.familywishes.entity.RelationshipSeed;
import com.familywishes.entity.SpecialEvent;
import com.familywishes.entity.TemplateTypeSeed;
import com.familywishes.exception.BadRequestException;
import com.familywishes.exception.NotFoundException;
import com.familywishes.repository.EventTypeSeedRepository;
import com.familywishes.repository.RelationshipSeedRepository;
import com.familywishes.repository.SpecialEventRepository;
import com.familywishes.repository.TemplateTypeSeedRepository;
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
  private final RelationshipSeedRepository relationshipSeedRepository;
  private final EventTypeSeedRepository eventTypeSeedRepository;
  private final TemplateTypeSeedRepository templateTypeSeedRepository;

  @Override
  public EnumSeedResponse createEnumSeed(String category, EnumSeedRequest request) {
    String code = normalizeCode(request.code());
    String display = request.displayName().trim();
    boolean active = request.active();
    return switch (normalizeCategory(category)) {
      case "RELATIONSHIP" -> {
        RelationshipSeed seed =
            relationshipSeedRepository
                .findByCode(code)
                .orElseGet(() -> RelationshipSeed.builder().code(code).build());
        seed.setDisplayName(display);
        seed.setActive(active);
        yield toResponse(relationshipSeedRepository.save(seed), "RELATIONSHIP");
      }
      case "EVENT_TYPE" -> {
        EventTypeSeed seed =
            eventTypeSeedRepository
                .findByCode(code)
                .orElseGet(() -> EventTypeSeed.builder().code(code).build());
        seed.setDisplayName(display);
        seed.setActive(active);
        yield toResponse(eventTypeSeedRepository.save(seed), "EVENT_TYPE");
      }
      case "TEMPLATE_TYPE" -> {
        TemplateTypeSeed seed =
            templateTypeSeedRepository
                .findByCode(code)
                .orElseGet(() -> TemplateTypeSeed.builder().code(code).build());
        seed.setDisplayName(display);
        seed.setActive(active);
        yield toResponse(templateTypeSeedRepository.save(seed), "TEMPLATE_TYPE");
      }
      default -> throw new BadRequestException("Unsupported enum category");
    };
  }

  @Override
  public EnumSeedResponse updateEnumSeed(String category, String code, EnumSeedRequest request) {
    String normalized = normalizeCategory(category);
    String targetCode = normalizeCode(code);
    String newCode = normalizeCode(request.code());
    String display = request.displayName().trim();

    return switch (normalized) {
      case "RELATIONSHIP" -> {
        RelationshipSeed seed =
            relationshipSeedRepository
                .findByCode(targetCode)
                .orElseThrow(() -> new NotFoundException("Enum seed not found"));
        seed.setCode(newCode);
        seed.setDisplayName(display);
        seed.setActive(request.active());
        yield toResponse(relationshipSeedRepository.save(seed), normalized);
      }
      case "EVENT_TYPE" -> {
        EventTypeSeed seed =
            eventTypeSeedRepository
                .findByCode(targetCode)
                .orElseThrow(() -> new NotFoundException("Enum seed not found"));
        seed.setCode(newCode);
        seed.setDisplayName(display);
        seed.setActive(request.active());
        yield toResponse(eventTypeSeedRepository.save(seed), normalized);
      }
      case "TEMPLATE_TYPE" -> {
        TemplateTypeSeed seed =
            templateTypeSeedRepository
                .findByCode(targetCode)
                .orElseThrow(() -> new NotFoundException("Enum seed not found"));
        seed.setCode(newCode);
        seed.setDisplayName(display);
        seed.setActive(request.active());
        yield toResponse(templateTypeSeedRepository.save(seed), normalized);
      }
      default -> throw new BadRequestException("Unsupported enum category");
    };
  }

  @Override
  public List<EnumSeedResponse> listEnumSeeds(String category) {
    return switch (normalizeCategory(category)) {
      case "RELATIONSHIP" -> relationshipSeedRepository.findByActiveTrueOrderByDisplayNameAsc().stream()
          .map(seed -> toResponse(seed, "RELATIONSHIP"))
          .toList();
      case "EVENT_TYPE" -> eventTypeSeedRepository.findByActiveTrueOrderByDisplayNameAsc().stream()
          .map(seed -> toResponse(seed, "EVENT_TYPE"))
          .toList();
      case "TEMPLATE_TYPE" -> templateTypeSeedRepository.findByActiveTrueOrderByDisplayNameAsc().stream()
          .map(seed -> toResponse(seed, "TEMPLATE_TYPE"))
          .toList();
      default -> throw new BadRequestException("Unsupported enum category");
    };
  }

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
    if (!specialEventRepository.existsById(id)) throw new NotFoundException("Special event seed not found");
    specialEventRepository.deleteById(id);
  }

  private String normalizeCode(String value) {
    return value.trim().toUpperCase();
  }

  private String normalizeCategory(String category) {
    return switch (normalizeCode(category)) {
      case "RELATIONSHIP", "EVENT_TYPE", "TEMPLATE_TYPE" -> normalizeCode(category);
      default -> throw new BadRequestException("Unsupported enum category");
    };
  }

  private EnumSeedResponse toResponse(RelationshipSeed seed, String category) {
    return new EnumSeedResponse(seed.getId(), category, seed.getCode(), seed.getDisplayName(), seed.isActive());
  }

  private EnumSeedResponse toResponse(EventTypeSeed seed, String category) {
    return new EnumSeedResponse(seed.getId(), category, seed.getCode(), seed.getDisplayName(), seed.isActive());
  }

  private EnumSeedResponse toResponse(TemplateTypeSeed seed, String category) {
    return new EnumSeedResponse(seed.getId(), category, seed.getCode(), seed.getDisplayName(), seed.isActive());
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
}
