package com.familywishes.service;

import com.familywishes.dto.EventDtos.*;

import java.util.List;

public interface EventService {
    EventResponse create(EventRequest request);
    List<EventResponse> list();
}
