package com.familywishes.service;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.EventDtos.*;

public interface EventService {
  EventResponse create(EventRequest request);

  PagedResponse<EventResponse> list(
      int page, int size, String searchKey, String sortBy, String sortDir);
}
