package com.familywishes.service.impl;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.UserDtos.*;
import com.familywishes.entity.User;
import com.familywishes.entity.UserWishSettings;
import com.familywishes.exception.NotFoundException;
import com.familywishes.repository.UserRepository;
import com.familywishes.service.UserService;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
  private final UserRepository userRepository;
  private final PasswordEncoder encoder;

  @Override
  public UserResponse create(UserRequest request) {
    User user =
        User.builder()
            .name(request.name())
            .email(request.email())
            .password(encoder.encode("Test"))
            .role(request.role())
            .active(true)
            .deleted(false)
            .build();
    if (request.isBirthdayEnabled()
        || request.isGoodNightEnabled()
        || request.isGoodMorningEnabled()) {
      UserWishSettings userWishSettings =
          UserWishSettings.builder()
              .user(user)
              .goodMorningEnabled(request.isGoodMorningEnabled())
              .goodNightEnabled(request.isGoodNightEnabled())
              .build();
      user.setWishSettings(userWishSettings);
    }
    user = userRepository.save(user);
    return getUserResponse(user);
  }

  private static UserResponse getUserResponse(User user) {
    if (Objects.nonNull(user.getWishSettings())) {
      return new UserResponse(
          user.getId(),
          user.getName(),
          user.getEmail(),
          user.getRole(),
          user.isActive(),
          user.getRelationShip().name(),
          user.getWishSettings().isGoodMorningEnabled(),
          user.getWishSettings().isGoodNightEnabled(),
          user.getWishSettings().isBirthdayEnabled());
    } else {
      return new UserResponse(
          user.getId(),
          user.getName(),
          user.getEmail(),
          user.getRole(),
          user.isActive(),
          user.getRelationShip().name(),
          false,
          false,
          false);
    }
  }

  @Override
  public UserResponse update(UserRequest request) {
    User user = userRepository.findByEmail(request.email()).orElse(null);
    user.setName(request.name());
    user.setRole(request.role());
    user.setRelationShip(request.relationShip());
    if (request.isBirthdayEnabled()
        || request.isGoodNightEnabled()
        || request.isGoodMorningEnabled()) {
      UserWishSettings userWishSettings =
          UserWishSettings.builder()
              .user(user)
              .goodMorningEnabled(request.isGoodMorningEnabled())
              .goodNightEnabled(request.isGoodNightEnabled())
              .build();
      user.setWishSettings(userWishSettings);
    }
    user = userRepository.save(user);
    return getUserResponse(user);
  }

  @Override
  public PagedResponse<UserResponse> list(
      int page, int size, String searchKey, String sortBy, String sortDir) {
    Sort.Direction direction =
        "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
    String normalizedSortBy = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy.trim();

    Page<User> userPage =
        userRepository.findAllActiveUsers(
            searchKey == null ? "" : searchKey.trim(),
            PageRequest.of(page, size, Sort.by(direction, normalizedSortBy)));

    return new PagedResponse<>(
        userPage.getContent().stream().map(UserServiceImpl::getUserResponse).toList(),
        userPage.getNumber(),
        userPage.getSize(),
        userPage.getTotalElements(),
        userPage.getTotalPages(),
        userPage.hasNext(),
        userPage.hasPrevious());
  }

  @Override
  public UserResponse getById(Long id) {
    User user =
        userRepository
            .findById(id)
            .orElseThrow(() -> new NotFoundException("User not found"));
    return getUserResponse(user);
  }

  @Override
  public UserResponse getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new NotFoundException("Authenticated user not found");
    }

    String email = authentication.getName();
    User user =
        userRepository
            .findByEmailAndDeletedFalse(email)
            .orElseThrow(() -> new NotFoundException("User not found"));
    return getUserResponse(user);
  }

  @Override
  public void deactivate(Long id) {
    User user =
        userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
    user.setActive(false);
    userRepository.save(user);
  }
}
