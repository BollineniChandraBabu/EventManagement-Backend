package com.familywishes.service.impl;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.SeedDtos.SpecialEventSeedRequest;
import com.familywishes.dto.SeedDtos.SpecialEventSeedResponse;
import com.familywishes.entity.SpecialEvent;
import com.familywishes.exception.NotFoundException;
import com.familywishes.repository.SpecialEventRepository;
import com.familywishes.service.SpecialEventSeedService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SpecialEventSeedServiceImpl implements SpecialEventSeedService {
  private final SpecialEventRepository specialEventRepository;

  @Override
  public SpecialEventSeedResponse create(SpecialEventSeedRequest request) {
    SpecialEvent event =
        SpecialEvent.builder()
            .eventName(request.eventName())
            .eventDate(request.eventDate())
            .message(request.message())
            .active(request.active())
            .build();
    return toResponse(specialEventRepository.save(event));
  }

  @Override
  public SpecialEventSeedResponse getById(Long id) {
    return toResponse(
        specialEventRepository
            .findById(id)
            .orElseThrow(() -> new NotFoundException("Special event seed not found")));
  }

  @Override
  public PagedResponse<SpecialEventSeedResponse> list(
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
  public List<SpecialEventSeedResponse> listTodayActive(LocalDate eventDate) {
    return specialEventRepository.findByEventDateAndActiveTrue(eventDate).stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  public SpecialEventSeedResponse update(Long id, SpecialEventSeedRequest request) {
    SpecialEvent event =
        specialEventRepository
            .findById(id)
            .orElseThrow(() -> new NotFoundException("Special event seed not found"));

    event.setEventName(request.eventName());
    event.setEventDate(request.eventDate());
    event.setMessage(request.message());
    event.setActive(request.active());

    return toResponse(specialEventRepository.save(event));
  }

  @Override
  public void delete(Long id) {
    if (!specialEventRepository.existsById(id)) {
      throw new NotFoundException("Special event seed not found");
    }
    specialEventRepository.deleteById(id);
  }

  private SpecialEventSeedResponse toResponse(SpecialEvent event) {
    return new SpecialEventSeedResponse(
        event.getId(),
        event.getEventName(),
        event.getEventDate(),
        event.getMessage(),
        event.isActive());
  }
}
