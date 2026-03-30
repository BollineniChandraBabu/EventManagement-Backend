package com.familywishes.service.impl;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.UserDtos.*;
import com.familywishes.entity.RelationshipSeed;
import com.familywishes.entity.User;
import com.familywishes.entity.UserWishSettings;
import com.familywishes.exception.BadRequestException;
import com.familywishes.exception.NotFoundException;
import com.familywishes.repository.RelationshipSeedRepository;
import com.familywishes.repository.UserRepository;
import com.familywishes.service.UserService;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
  private final UserRepository userRepository;
  private final PasswordEncoder encoder;
  private final RelationshipSeedRepository relationshipSeedRepository;

  @Override
  public UserResponse create(UserRequest request) {
    User user =
        User.builder()
            .name(request.name())
            .email(request.email())
            .password(encoder.encode("Test"))
            .role(request.role())
            .birthday(request.dateOfBirth())
            .relationShip(resolveRelationship(request.relationShip()))
            .active(true)
            .deleted(false)
            .build();

    user.setWishSettings(
        UserWishSettings.builder()
            .user(user)
            .goodMorningEnabled(request.isGoodMorningEnabled())
            .goodNightEnabled(request.isGoodNightEnabled())
            .birthdayEnabled(request.isBirthdayEnabled())
            .build());

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
          user.getBirthday(),
          user.isActive(),
          user.getRelationShip().getCode(),
          user.getWishSettings().isGoodMorningEnabled(),
          user.getWishSettings().isGoodNightEnabled(),
          user.getWishSettings().isBirthdayEnabled(),
          user.isOnline(),
          user.getLastSeenAt());
    } else {
      return new UserResponse(
          user.getId(),
          user.getName(),
          user.getEmail(),
          user.getRole(),
          user.getBirthday(),
          user.isActive(),
          user.getRelationShip().getCode(),
          false,
          false,
          false,
          user.isOnline(),
          user.getLastSeenAt());
    }
  }

  @Override
  public UserResponse update(UserRequest request) {
    User user =
        userRepository
            .findByEmail(request.email())
            .orElseThrow(() -> new NotFoundException("User not found"));
    user.setName(request.name());
    if (!user.getRole().equals(request.role()) && !isAdminAuthenticated()) {
      throw new BadRequestException("Only admin can update role");
    }
    user.setRole(request.role());
    user.setBirthday(request.dateOfBirth());
    user.setRelationShip(resolveRelationship(request.relationShip()));

    UserWishSettings userWishSettings =
        user.getWishSettings() == null
            ? UserWishSettings.builder().user(user).build()
            : user.getWishSettings();
    userWishSettings.setGoodMorningEnabled(request.isGoodMorningEnabled());
    userWishSettings.setGoodNightEnabled(request.isGoodNightEnabled());
    userWishSettings.setBirthdayEnabled(request.isBirthdayEnabled());
    user.setWishSettings(userWishSettings);

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
        userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
    return getUserResponse(user);
  }

  @Override
  public UserResponse getCurrentUser() {
    User user = findAuthenticatedUser();
    return getUserResponse(user);
  }

  @Override
  public UserResponse updateCurrentUserWishSettings(WishSettingsUpdateRequest request) {
    User user = findAuthenticatedUser();

    UserWishSettings settings =
        user.getWishSettings() == null
            ? UserWishSettings.builder().user(user).build()
            : user.getWishSettings();

    if (request.isGoodMorningEnabled() != null)
      settings.setGoodMorningEnabled(request.isGoodMorningEnabled());
    if (request.isGoodNightEnabled() != null)
      settings.setGoodNightEnabled(request.isGoodNightEnabled());
    if (request.isBirthdayEnabled() != null)
      settings.setBirthdayEnabled(request.isBirthdayEnabled());

    user.setWishSettings(settings);
    user = userRepository.save(user);
    return getUserResponse(user);
  }

  @Override
  public void deactivate(Long id) {
    updateStatus(id, false);
  }

  @Override
  public UserResponse updateStatus(Long id, boolean active) {
    User user =
        userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
    user.setActive(active);
    user = userRepository.save(user);
    return getUserResponse(user);
  }

  private RelationshipSeed resolveRelationship(String relationship) {
    return relationshipSeedRepository
        .findByCodeAndActiveTrue(relationship.trim().toUpperCase())
        .orElseThrow(() -> new BadRequestException("Invalid relationship"));
  }

  private User findAuthenticatedUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new NotFoundException("Authenticated user not found");
    }

    String email = authentication.getName();
    return userRepository
        .findByEmailAndDeletedFalse(email)
        .orElseThrow(() -> new NotFoundException("User not found"));
  }

  private boolean isAdminAuthenticated() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication.getAuthorities() == null) {
      return false;
    }

    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch("ROLE_ADMIN"::equals);
  }
}
