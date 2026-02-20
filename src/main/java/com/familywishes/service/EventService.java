package com.familywishes.service;

import com.familywishes.dto.EventDtos.*;
import com.familywishes.dto.CommonDtos.PagedResponse;

public interface EventService {
    EventResponse create(EventRequest request);
    PagedResponse<EventResponse> list(int page, int size, String searchKey);
}
