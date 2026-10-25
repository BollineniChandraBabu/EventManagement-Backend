package com.familywishes.controller;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.WishImageDtos.WishImageResponse;
import com.familywishes.service.WishImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/wish-images")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class WishImageAdminController {
  private final WishImageService wishImageService;

  @PostMapping(consumes = "multipart/form-data")
  @ResponseStatus(HttpStatus.CREATED)
  public WishImageResponse create(
      @RequestParam(required = false) Long userId,
      @RequestParam String eventType,
      @RequestParam(defaultValue = "true") boolean active,
      @RequestPart MultipartFile file) {
    return wishImageService.create(userId, eventType, active, file);
  }

  @PutMapping(value = "/{id}", consumes = "multipart/form-data")
  public WishImageResponse update(
      @PathVariable Long id,
      @RequestParam(required = false) Long userId,
      @RequestParam(defaultValue = "false") boolean clearUser,
      @RequestParam(required = false) String eventType,
      @RequestParam(required = false) Boolean active,
      @RequestPart(required = false) MultipartFile file) {
    return wishImageService.update(id, userId, clearUser, eventType, active, file);
  }

  @GetMapping("/filter-values")
  public java.util.Map<String, Object> getFilterValues() {
    java.util.List<Boolean> activeStatuses = java.util.List.of(true, false);
    return java.util.Map.of("active", activeStatuses);
  }

  @GetMapping
  public PagedResponse<WishImageResponse> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "") String searchKey,
      @RequestParam(required = false) String eventType,
      @RequestParam(required = false) Long userId,
      @RequestParam(required = false) java.util.List<String> eventTypes,
      @RequestParam(required = false) java.util.List<Long> userIds,
      @RequestParam(required = false) Boolean active,
      @RequestParam(defaultValue = "id") String sortBy,
      @RequestParam(defaultValue = "desc") String sortDir) {
    if ((eventTypes != null && !eventTypes.isEmpty()) || (userIds != null && !userIds.isEmpty())) {
      return wishImageService.list(page, size, searchKey, eventTypes, userIds, active, sortBy, sortDir);
    }
    return wishImageService.list(page, size, searchKey, eventType == null ? "" : eventType, userId, active, sortBy, sortDir);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    wishImageService.delete(id);
  }
}
