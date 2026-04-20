package com.familywishes.service;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.UserDtos.*;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
  UserResponse create(UserRequest request);

  UserResponse update(UserRequest request);

  PagedResponse<UserResponse> list(
      int page, int size, String searchKey, String sortBy, String sortDir);

  UserResponse getById(Long id);

  UserResponse getCurrentUser();

  UserResponse updateCurrentUserWishSettings(WishSettingsUpdateRequest request);

  WishPreviewResponse getCurrentUserWishPreview();

  ProfilePictureUploadUrlResponse getCurrentUserProfilePictureUploadUrl(
      ProfilePictureUploadUrlRequest request);

  UserResponse uploadCurrentUserProfilePicture(MultipartFile file);

  UserResponse removeCurrentUserProfilePicture();

  void deactivate(Long id);

  UserResponse updateStatus(Long id, boolean active);
}
