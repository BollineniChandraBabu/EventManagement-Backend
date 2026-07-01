package com.familywishes.service;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.NotificationDtos.NotificationRequest;
import com.familywishes.dto.NotificationDtos.NotificationResponse;

public interface NotificationService {
  NotificationResponse create(NotificationRequest request);

  NotificationResponse update(Long id, NotificationRequest request);

  NotificationResponse getById(Long id);

  PagedResponse<NotificationResponse> getAll(
      int page, int size, String searchKey, String sortBy, String sortDir);

  void delete(Long id);

  NotificationResponse publish(Long id);

  NotificationResponse unpublish(Long id);

  NotificationResponse getPublished();
}
