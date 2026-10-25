package com.familywishes.controller;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.UserDtos.*;
import com.familywishes.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

  @GetMapping("/filter-values")
  @PreAuthorize("hasRole('ADMIN')")
  public java.util.Map<String, Object> getFilterValues() {
    return java.util.Map.of(
        "roles", java.util.Arrays.stream(com.familywishes.entity.enums.Role.values()).map(Enum::name).toList(),
        "genders", java.util.Arrays.stream(com.familywishes.entity.enums.Gender.values()).map(Enum::name).toList(),
        "active", java.util.List.of(true, false));
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public PagedResponse<UserResponse> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "") String searchKey,
      @RequestParam(required = false) java.util.List<com.familywishes.entity.enums.Role> roles,
      @RequestParam(required = false) java.util.List<com.familywishes.entity.enums.Gender> genders,
      @RequestParam(required = false) java.util.List<String> relationships,
      @RequestParam(required = false) Boolean active,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "desc") String sortDir) {
    return userService.list(
        page, size, searchKey, roles, genders, relationships, active, sortBy, sortDir);
  }

  @GetMapping("/me")
  public UserResponse me() {
    return userService.getCurrentUser();
  }

  @PatchMapping("/me/wish-settings")
  public UserResponse updateMyWishSettings(@RequestBody WishSettingsUpdateRequest request) {
    return userService.updateCurrentUserWishSettings(request);
  }

  @GetMapping("/me/wish-preview")
  public WishPreviewResponse myWishPreview() {
    return userService.getCurrentUserWishPreview();
  }

  @PostMapping("/me/profile-picture/presigned-url")
  public ProfilePictureUploadUrlResponse getMyProfilePictureUploadUrl(
      @RequestBody(required = false) ProfilePictureUploadUrlRequest request) {
    return userService.getCurrentUserProfilePictureUploadUrl(request);
  }

  @PostMapping("/me/profile-picture")
  public UserResponse uploadMyProfilePicture(@RequestPart("file") MultipartFile file) {
    return userService.uploadCurrentUserProfilePicture(file);
  }

  @DeleteMapping("/me/profile-picture")
  public UserResponse removeMyProfilePicture() {
    return userService.removeCurrentUserProfilePicture();
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
