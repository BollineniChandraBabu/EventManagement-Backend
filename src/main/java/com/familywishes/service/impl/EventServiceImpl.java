package com.familywishes.service.impl;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.EventDtos.*;
import com.familywishes.entity.Event;
import com.familywishes.entity.EventTypeSeed;
import com.familywishes.exception.BadRequestException;
import com.familywishes.exception.NotFoundException;
import com.familywishes.repository.EventRepository;
import com.familywishes.repository.EventTypeSeedRepository;
import com.familywishes.repository.UserRepository;
import com.familywishes.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
  private final EventRepository eventRepository;
  private final UserRepository userRepository;
  private final EventTypeSeedRepository eventTypeSeedRepository;

  @Override
  public EventResponse create(EventRequest request) {
    var user =
        userRepository
            .findById(request.userId())
            .orElseThrow(() -> new NotFoundException("User not found"));
    Event event =
        Event.builder()
            .eventType(resolveEventType(request.eventType()))
            .festivalName(request.festivalName())
            .eventDate(request.eventDate())
            .recurring(request.recurring())
            .user(user)
            .active(true)
            .build();
    event = eventRepository.save(event);
    return toResponse(event);
  }

  @Override
  public EventResponse getById(Long id) {
    Event event =
        eventRepository.findById(id).orElseThrow(() -> new NotFoundException("Event not found"));
    return toResponse(event);
  }

  @Override
  public PagedResponse<EventResponse> list(
      int page, int size, String searchKey, String sortBy, String sortDir) {
    Sort.Direction direction =
        "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
    String normalizedSortBy = (sortBy == null || sortBy.isBlank()) ? "id" : sortBy.trim();

    Page<Event> events =
        eventRepository.findAllBySearchKey(
            searchKey == null ? "" : searchKey.trim(),
            PageRequest.of(page, size, Sort.by(direction, normalizedSortBy)));

    return new PagedResponse<>(
        events.getContent().stream().map(this::toResponse).toList(),
        events.getNumber(),
        events.getSize(),
        events.getTotalElements(),
        events.getTotalPages(),
        events.hasNext(),
        events.hasPrevious());
  }

  private EventTypeSeed resolveEventType(String eventType) {
    return eventTypeSeedRepository
        .findByCodeAndActiveTrue(eventType.trim().toUpperCase())
        .orElseThrow(() -> new BadRequestException("Invalid event type"));
  }

  private EventResponse toResponse(Event event) {
    return new EventResponse(
        event.getId(),
        event.getEventType().getCode(),
        event.getFestivalName(),
        event.getEventDate(),
        event.isRecurring(),
        event.getUser().getId(),
        event.isActive());
  }
}
