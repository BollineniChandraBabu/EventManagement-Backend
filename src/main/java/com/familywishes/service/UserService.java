package com.familywishes.service;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.UserDtos.*;

public interface UserService {
  UserResponse create(UserRequest request);

  UserResponse update(UserRequest request);

  PagedResponse<UserResponse> list(
      int page, int size, String searchKey, String sortBy, String sortDir);

  UserResponse getById(Long id);

  UserResponse getCurrentUser();

  UserResponse updateCurrentUserWishSettings(WishSettingsUpdateRequest request);

  void deactivate(Long id);

  UserResponse updateStatus(Long id, boolean active);
}
