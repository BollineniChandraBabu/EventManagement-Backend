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

  @GetMapping
  public PagedResponse<WishImageResponse> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "") String searchKey,
      @RequestParam(defaultValue = "") String eventType,
      @RequestParam(required = false) Long userId,
      @RequestParam(required = false) Boolean active,
      @RequestParam(defaultValue = "id") String sortBy,
      @RequestParam(defaultValue = "desc") String sortDir) {
    return wishImageService.list(page, size, searchKey, eventType, userId, active, sortBy, sortDir);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    wishImageService.delete(id);
  }
}
