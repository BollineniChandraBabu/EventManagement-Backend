package com.familywishes.service;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.EventDtos.*;

import java.util.List;

public interface EventService {
  EventResponse create(EventRequest request);

  EventResponse getById(Long id);

  void deleteById(Long id);

  PagedResponse<EventResponse> list(
      int page,
      int size,
      String searchKey,
      String sortBy,
      String sortDir,
      boolean isAdmin,
      String userEmail);

  List<EventResponse> listByMonth(Integer month);

  List<EventResponse> listByMonthAndUserEmail(Integer month, String userEmail);
}
