package com.familywishes.service;

import com.familywishes.dto.UserDtos.*;

import java.util.List;

public interface UserService {
    UserResponse create(UserRequest request);
    UserResponse update(UserRequest request);
    List<UserResponse> list();
    UserResponse getCurrentUser();
    void deactivate(Long id);
}