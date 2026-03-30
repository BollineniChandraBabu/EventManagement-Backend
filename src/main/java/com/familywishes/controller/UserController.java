package com.familywishes.controller;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.UserDtos.*;
import com.familywishes.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public UserResponse create(@Valid @RequestBody UserRequest request) {
    return userService.create(request);
  }

  @PutMapping
  @PreAuthorize("hasRole('ADMIN')")
  public UserResponse update(@Valid @RequestBody UserRequest request) {
    return userService.update(request);
  }

  @GetMapping("/{id}")
  public UserResponse getById(@PathVariable Long id) {
    return userService.getById(id);
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public PagedResponse<UserResponse> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "") String searchKey,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "desc") String sortDir) {
    return userService.list(page, size, searchKey, sortBy, sortDir);
  }

  @GetMapping("/me")
  public UserResponse me() {
    return userService.getCurrentUser();
  }

  @PatchMapping("/me/wish-settings")
  public UserResponse updateMyWishSettings(@RequestBody WishSettingsUpdateRequest request) {
    return userService.updateCurrentUserWishSettings(request);
  }

  @PostMapping("/{id}/deactivate")
  @PreAuthorize("hasRole('ADMIN')")
  public void deactivate(@PathVariable Long id) {
    userService.deactivate(id);
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasRole('ADMIN')")
  public UserResponse updateStatus(
      @PathVariable Long id, @Valid @RequestBody UserStatusUpdateRequest request) {
    return userService.updateStatus(id, request.active());
  }
}
