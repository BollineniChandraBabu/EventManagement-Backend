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
import com.familywishes.util.MediaValidation;
import com.familywishes.util.WishesUrls;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
  private final EventRepository eventRepository;
  private final UserRepository userRepository;
  private final EventTypeSeedRepository eventTypeSeedRepository;
  private final SupabaseStorageService storageService;

  @Override
  public EventResponse create(EventRequest request) {
    return create(request, null);
  }

  @Override
  public EventResponse create(EventRequest request, MultipartFile image) {
    var user =
        userRepository
            .findById(request.userId())
            .orElseThrow(() -> new NotFoundException("User not found"));
    Event event =
        Event.builder()
            .eventType(resolveEventType(request.eventType()))
            .eventDate(request.eventDate())
            .recurring(request.recurring())
            .wishesUrl(normalizeWishesUrl(request.wishesUrl()))
            .imageObjectKey(uploadImage(image))
            .user(user)
            .active(true)
            .build();
    event = eventRepository.save(event);
    return toResponse(event);
  }

  @Override
  public EventResponse update(Long id, EventRequest request) {
    return update(id, request, null);
  }

  @Override
  public EventResponse update(Long id, EventRequest request, MultipartFile image) {
    Event event = findEvent(id);
    var user =
        userRepository
            .findById(request.userId())
            .orElseThrow(() -> new NotFoundException("User not found"));
    event.setEventType(resolveEventType(request.eventType()));
    event.setEventDate(request.eventDate());
    event.setRecurring(request.recurring());
    event.setWishesUrl(normalizeWishesUrl(request.wishesUrl())); // blank removes the link
    event.setUser(user);
    if (image != null && !image.isEmpty()) {
      String oldObjectKey = event.getImageObjectKey();
      event.setImageObjectKey(uploadImage(image));
      storageService.deleteObject(oldObjectKey);
    }
    return toResponse(eventRepository.save(event));
  }

  @Override
  public EventResponse getById(Long id) {
    return toResponse(findEvent(id));
  }

  @Override
  public EventResponse getById(Long id, boolean isAdmin, String requesterEmail) {
    Event event = findEvent(id);
    boolean isOwner =
        event.getUser().getEmail() != null
            && event.getUser().getEmail().equalsIgnoreCase(requesterEmail);
    if (!isAdmin && !isOwner) {
      throw new NotFoundException("Event not found"); // do not reveal that it exists
    }
    return toResponse(event);
  }

  @Override
  public void deleteById(Long id) {
    Event event = findEvent(id);
    eventRepository.deleteById(id);
    storageService.deleteObject(event.getImageObjectKey());
  }

  @Override
  public void deleteByIds(List<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      throw new BadRequestException("At least one event id is required");
    }
    ids.stream().distinct().forEach(this::deleteById);
  }

  @Override
  public PagedResponse<EventResponse> list(
      int page,
      int size,
      String searchKey,
      List<String> eventTypes,
      List<Long> userIds,
      Boolean active,
      String sortBy,
      String sortDir,
      boolean isAdmin,
      String userEmail) {
    Sort.Direction direction =
        "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
    String normalizedSortBy = (sortBy == null || sortBy.isBlank()) ? "id" : sortBy.trim();
    List<String> cleanTypes = (eventTypes == null || eventTypes.isEmpty()) ? null : eventTypes;
    List<Long> cleanUserIds = (userIds == null || userIds.isEmpty()) ? null : userIds;

    Page<Event> events;
    if (isAdmin) {
      events =
          eventRepository.findAllByFilters(
              searchKey == null ? "" : searchKey.trim(),
              cleanTypes,
              cleanUserIds,
              Event.toActiveFilter(active),
              PageRequest.of(page, size, Sort.by(direction, normalizedSortBy)));
    } else {
      events =
          eventRepository.findAllByFiltersAndUser(
              userEmail,
              searchKey == null ? "" : searchKey.trim(),
              cleanTypes,
              Event.toActiveFilter(active),
              PageRequest.of(page, size, Sort.by(direction, normalizedSortBy)));
    }

    return new PagedResponse<>(
        events.getContent().stream().map(this::toResponse).toList(),
        events.getNumber(),
        events.getSize(),
        events.getTotalElements(),
        events.getTotalPages(),
        events.hasNext(),
        events.hasPrevious());
  }

  @Override
  public List<EventResponse> listByMonth(Integer month) {
    return eventRepository.findByMonth(month).stream()
        .map(
            event -> {
              return new EventResponse(
                  event.getId(),
                  event.getEventType().getDisplayName(),
                  event.getEventDate(),
                  event.isRecurring(),
                  event.getUser().getName(),
                  event.isActive(),
                  event.getUser().getId(),
                  event.getWishesUrl(),
                  imageUrl(event));
            })
        .toList();
  }

  @Override
  public List<EventResponse> listByMonthAndUserEmail(Integer month, String userEmail) {
    return eventRepository.findByMonthAndUserEmail(month, userEmail).stream()
        .map(
            event -> {
              return new EventResponse(
                  event.getId(),
                  event.getEventType().getDisplayName(),
                  event.getEventDate(),
                  event.isRecurring(),
                  event.getUser().getName(),
                  event.isActive(),
                  event.getUser().getId(),
                  event.getWishesUrl(),
                  imageUrl(event));
            })
        .toList();
  }

  private EventTypeSeed resolveEventType(String eventType) {
    return eventTypeSeedRepository
        .findByCodeAndActiveTrue(eventType.trim().toUpperCase())
        .orElseThrow(() -> new BadRequestException("Invalid event type"));
  }

  private Event findEvent(Long id) {
    return eventRepository.findById(id).orElseThrow(() -> new NotFoundException("Event not found"));
  }

  /** Blank means "no link"; a bad link is reported to the user as a 400. */
  private static String normalizeWishesUrl(String raw) {
    try {
      return WishesUrls.normalizeOrNull(raw);
    } catch (IllegalArgumentException e) {
      throw new BadRequestException(e.getMessage());
    }
  }

  /** A null or empty upload means the event keeps whatever image it already had. */
  private String uploadImage(MultipartFile image) {
    if (image == null || image.isEmpty()) {
      return null;
    }
    MediaValidation.validate(image);
    String objectKey = storageService.uploadWishImage(image);
    if (!StringUtils.hasText(objectKey)) {
      throw new BadRequestException("Unable to upload image to storage");
    }
    return objectKey;
  }

  private String imageUrl(Event event) {
    return StringUtils.hasText(event.getImageObjectKey())
        ? storageService.getSignedUrl(event.getImageObjectKey())
        : null;
  }

  private EventResponse toResponse(Event event) {
    return new EventResponse(
        event.getId(),
        event.getEventType().getCode(),
        event.getEventDate(),
        event.isRecurring(),
        event.getUser().getName(),
        event.isActive(),
        event.getUser().getId(),
        event.getWishesUrl(),
        imageUrl(event));
  }
}
