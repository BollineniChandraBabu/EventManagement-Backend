package com.familywishes.service.impl;

import com.familywishes.dto.EventDtos.*;
import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.entity.Event;
import com.familywishes.exception.NotFoundException;
import com.familywishes.repository.EventRepository;
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

    @Override
    public EventResponse create(EventRequest request) {
        var user = userRepository.findById(request.userId()).orElseThrow(() -> new NotFoundException("User not found"));
        Event event = Event.builder().subject(request.subject()).body(request.body()).eventType(request.eventType()).festivalName(request.festivalName())
                .eventDate(request.eventDate()).recurring(request.recurring()).user(user).active(true).build();
        event = eventRepository.save(event);
        return new EventResponse(event.getId(), event.getSubject(), event.getBody(), event.getEventType(), event.getFestivalName(), event.getEventDate(), event.isRecurring(), event.getUser().getId(), event.isActive());
    }

    @Override
    public PagedResponse<EventResponse> list(int page, int size, String searchKey) {
        Page<Event> events = eventRepository.findAllBySearchKey(
                searchKey == null ? "" : searchKey.trim(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))
        );

        return new PagedResponse<>(
                events.getContent().stream().map(e -> new EventResponse(e.getId(), e.getSubject(), e.getBody(), e.getEventType(), e.getFestivalName(), e.getEventDate(), e.isRecurring(), e.getUser().getId(), e.isActive())).toList(),
                events.getNumber(),
                events.getSize(),
                events.getTotalElements(),
                events.getTotalPages(),
                events.hasNext(),
                events.hasPrevious()
        );
    }
}
