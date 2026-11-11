package com.familywishes.service;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.UserDtos.*;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
  UserResponse create(UserRequest request);

  UserResponse update(UserRequest request);

  PagedResponse<UserResponse> list(
      int page,
      int size,
      String searchKey,
      List<com.familywishes.entity.enums.Role> roles,
      List<com.familywishes.entity.enums.Gender> genders,
      List<String> relationships,
      Boolean active,
      String sortBy,
      String sortDir);

  UserResponse getById(Long id);

  PagedResponse<DirectoryUserResponse> directory(
      int page, int size, String searchKey, String sortBy, String sortDir);

  UserResponse getCurrentUser();

  UserResponse updateCurrentUserWishSettings(WishSettingsUpdateRequest request);

  UserResponse updateCurrentUserMfa(MfaUpdateRequest request);

  UserResponse updateCurrentUserConcurrentLogin(ConcurrentLoginUpdateRequest request);

  UserResponse updateConcurrentLogin(Long id, ConcurrentLoginUpdateRequest request);

  WishPreviewResponse getCurrentUserWishPreview();

  ProfilePictureUploadUrlResponse getCurrentUserProfilePictureUploadUrl(
      ProfilePictureUploadUrlRequest request);

  UserResponse uploadCurrentUserProfilePicture(MultipartFile file);

  UserResponse removeCurrentUserProfilePicture();

  void deactivate(Long id);

  void deactivateByIds(List<Long> ids);

  UserResponse updateStatus(Long id, boolean active);

  List<UserResponse> listByMonth(Integer month);

  List<UserResponse> listByMonthAndUserEmail(Integer month, String userEmail);

  /**
   * Birthdays of the given users for a month. Used to show a user the birthdays of the people in
   * their circle, and returns an empty list when no users are given.
   */
  List<UserResponse> listByMonthForUsers(Integer month, List<Long> userIds);
}
