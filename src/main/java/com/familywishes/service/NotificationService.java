package com.familywishes.service;

import com.familywishes.dto.NotificationDtos.NotificationRequest;
import com.familywishes.dto.NotificationDtos.NotificationResponse;
import java.util.List;

public interface NotificationService {
  NotificationResponse create(NotificationRequest request);

  NotificationResponse update(Long id, NotificationRequest request);

  NotificationResponse getById(Long id);

  List<NotificationResponse> getAll();

  void delete(Long id);

  NotificationResponse publish(Long id);
}
