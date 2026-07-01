package com.familywishes.controller;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.NotificationDtos.NotificationRequest;
import com.familywishes.dto.NotificationDtos.NotificationResponse;
import com.familywishes.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class NotificationController {
  private final NotificationService notificationService;

  @PostMapping
  public NotificationResponse create(@Valid @RequestBody NotificationRequest request) {
    return notificationService.create(request);
  }

  @GetMapping("/{id}")
  public NotificationResponse getById(@PathVariable Long id) {
    return notificationService.getById(id);
  }

  @GetMapping
  public PagedResponse<NotificationResponse> getAll(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "") String searchKey,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "desc") String sortDir) {
    return notificationService.getAll(page, size, searchKey, sortBy, sortDir);
  }

  @PutMapping("/{id}")
  public NotificationResponse update(
      @PathVariable Long id, @Valid @RequestBody NotificationRequest request) {
    return notificationService.update(id, request);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    notificationService.delete(id);
  }

  @PostMapping("/{id}/publish")
  public NotificationResponse publish(@PathVariable Long id) {
    return notificationService.publish(id);
  }

  @PostMapping("/{id}/unpublish")
  public NotificationResponse unpublish(@PathVariable Long id) {
    return notificationService.unpublish(id);
  }
}
