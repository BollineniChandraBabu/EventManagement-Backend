package com.familywishes.service;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.UserDtos.*;

public interface UserService {
  UserResponse create(UserRequest request);

  UserResponse update(UserRequest request);

  PagedResponse<UserResponse> list(int page, int size, String searchKey);

  UserResponse getCurrentUser();

  void deactivate(Long id);
}
