package com.familywishes.service.impl;

import com.familywishes.dto.EventDtos.*;
import com.familywishes.entity.Event;
import com.familywishes.exception.NotFoundException;
import com.familywishes.repository.EventRepository;
import com.familywishes.repository.UserRepository;
import com.familywishes.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Override
    public EventResponse create(EventRequest request) {
        var user = userRepository.findById(request.userId()).orElseThrow(() -> new NotFoundException("User not found"));
        Event event = Event.builder().title(request.title()).eventType(request.eventType()).festivalName(request.festivalName())
                .eventDate(request.eventDate()).recurring(request.recurring()).user(user).active(true).build();
        event = eventRepository.save(event);
        return new EventResponse(event.getId(), event.getTitle(), event.getEventType(), event.getFestivalName(), event.getEventDate(), event.isRecurring(), event.getUser().getId(), event.isActive());
    }

    @Override
    public List<EventResponse> list() {
        return eventRepository.findAll().stream().map(e -> new EventResponse(e.getId(), e.getTitle(), e.getEventType(), e.getFestivalName(), e.getEventDate(), e.isRecurring(), e.getUser().getId(), e.isActive())).toList();
    }
}
