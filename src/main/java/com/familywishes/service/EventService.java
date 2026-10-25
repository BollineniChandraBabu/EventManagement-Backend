package com.familywishes.service;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.EventDtos.*;

import java.util.List;

public interface EventService {
  EventResponse create(EventRequest request);

  EventResponse getById(Long id);

  /** Admins may read any event; everyone else only their own. */
  EventResponse getById(Long id, boolean isAdmin, String requesterEmail);

  EventResponse update(Long id, EventRequest request);

  void deleteById(Long id);

  PagedResponse<EventResponse> list(
      int page,
      int size,
      String searchKey,
      List<String> eventTypes,
      List<Long> userIds,
      Boolean active,
      String sortBy,
      String sortDir,
      boolean isAdmin,
      String userEmail);

  List<EventResponse> listByMonth(Integer month);

  List<EventResponse> listByMonthAndUserEmail(Integer month, String userEmail);
}
