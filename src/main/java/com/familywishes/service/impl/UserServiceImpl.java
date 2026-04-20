package com.familywishes.service.impl;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.AiWishRequest;
import com.familywishes.dto.AiWishResponse;
import com.familywishes.dto.UserDtos.*;
import com.familywishes.entity.FestivalWishMapping;
import com.familywishes.entity.RelationshipSeed;
import com.familywishes.entity.User;
import com.familywishes.entity.UserWishSettings;
import com.familywishes.exception.BadRequestException;
import com.familywishes.exception.NotFoundException;
import com.familywishes.repository.RelationshipSeedRepository;
import com.familywishes.repository.FestivalWishMappingRepository;
import com.familywishes.repository.UserRepository;
import com.familywishes.service.AiService;
import com.familywishes.service.UserService;
import java.time.LocalDate;
import java.time.ZoneId;
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
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
  private final UserRepository userRepository;
  private final PasswordEncoder encoder;
  private final RelationshipSeedRepository relationshipSeedRepository;
  private final SupabaseStorageService supabaseStorageService;
  private final FestivalWishMappingRepository festivalWishMappingRepository;
  private final AiService aiService;

  @Override
  public UserResponse create(UserRequest request) {
    User user =
        User.builder()
            .name(request.name())
            .email(request.email())
            .password(encoder.encode("Test"))
            .role(request.role())
            .gender(request.gender())
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
    return toUserResponse(user);
  }

  private UserResponse toUserResponse(User user) {
    if (Objects.nonNull(user.getWishSettings())) {
      return new UserResponse(
          user.getId(),
          user.getName(),
          user.getEmail(),
          user.getRole(),
          user.getGender(),
          user.getBirthday(),
          user.isActive(),
          user.getRelationShip().getCode(),
          user.getWishSettings().isGoodMorningEnabled(),
          user.getWishSettings().isGoodNightEnabled(),
          user.getWishSettings().isBirthdayEnabled(),
          user.isOnline(),
          user.getLastSeenAt(),
          resolveProfilePictureUrl(user));
    } else {
      return new UserResponse(
          user.getId(),
          user.getName(),
          user.getEmail(),
          user.getRole(),
          user.getGender(),
          user.getBirthday(),
          user.isActive(),
          user.getRelationShip().getCode(),
          false,
          false,
          false,
          user.isOnline(),
          user.getLastSeenAt(),
          resolveProfilePictureUrl(user));
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
    user.setGender(request.gender());
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
    return toUserResponse(user);
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
        userPage.getContent().stream().map(this::toUserResponse).toList(),
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
    return toUserResponse(user);
  }

  @Override
  public UserResponse getCurrentUser() {
    User user = findAuthenticatedUser();
    return toUserResponse(user);
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
    return toUserResponse(user);
  }

  @Override
  public WishPreviewResponse getCurrentUserWishPreview() {
    User user = findAuthenticatedUser();
    if (user.getWishSettings() == null) {
      return new WishPreviewResponse(false, null, null, null, null);
    }

    FestivalWishMapping selectedFestival =
        festivalWishMappingRepository.findByUser_IdAndActiveTrue(user.getId()).stream()
            .filter(m -> m.getSpecialEvent() != null && m.getSpecialEvent().isActive())
            .sorted(
                java.util.Comparator.comparing(
                    m -> daysUntil(m.getSpecialEvent().getEventDate(), LocalDate.now(ZoneId.of("Asia/Kolkata")))))
            .findFirst()
            .orElse(null);

    boolean birthdayEnabled = user.getWishSettings().isBirthdayEnabled();
    boolean festivalEnabled = selectedFestival != null;

    if (!birthdayEnabled && !festivalEnabled) {
      return new WishPreviewResponse(false, null, null, null, null);
    }

    try {
      AiWishRequest request;
      String wishType;
      if (festivalEnabled) {
        request =
            new AiWishRequest(
                user.getName(),
                user.getRelationShip().getCode(),
                "",
                selectedFestival.getSpecialEvent().getEventName(),
                "Emotional",
                "EN");
        wishType = "FESTIVAL";
      } else {
        request =
            new AiWishRequest(
                user.getName(),
                user.getRelationShip().getCode(),
                "BIRTHDAY",
                "",
                "Emotional",
                "EN");
        wishType = "BIRTHDAY";
      }

      AiWishResponse aiWishResponse = aiService.generate(request);
      byte[] image = aiService.callGeminiImage(request);
      return new WishPreviewResponse(
          true, wishType, aiWishResponse.subject(), aiWishResponse.htmlMessage(), image);
    } catch (Exception ex) {
      throw new BadRequestException("Unable to generate wish preview");
    }
  }

  @Override
  public UserResponse uploadCurrentUserProfilePicture(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BadRequestException("Profile picture file is required");
    }
    User user = findAuthenticatedUser();
    if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isBlank()) {
      supabaseStorageService.deleteByPublicUrl(user.getProfilePictureUrl());
    }
    String uploadedUrl = supabaseStorageService.uploadUserProfilePicture(file, user.getId());
    if (uploadedUrl == null || uploadedUrl.isBlank()) {
      throw new BadRequestException("Unable to upload profile picture");
    }
    user.setProfilePictureUrl(uploadedUrl);
    user = userRepository.save(user);
    return toUserResponse(user);
  }

  @Override
  public UserResponse removeCurrentUserProfilePicture() {
    User user = findAuthenticatedUser();
    if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isBlank()) {
      supabaseStorageService.deleteByPublicUrl(user.getProfilePictureUrl());
    }
    user.setProfilePictureUrl(null);
    user = userRepository.save(user);
    return toUserResponse(user);
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
    return toUserResponse(user);
  }

  private String resolveProfilePictureUrl(User user) {
    if (user.getProfilePictureUrl() == null || user.getProfilePictureUrl().isBlank()) {
      return supabaseStorageService.getDefaultProfilePictureUrl(user.getGender());
    }
    return user.getProfilePictureUrl();
  }

  private RelationshipSeed resolveRelationship(String relationship) {
    return relationshipSeedRepository
        .findByCodeAndActiveTrue(relationship.trim().toUpperCase())
        .orElseThrow(() -> new BadRequestException("Invalid relationship"));
  }

  private long daysUntil(LocalDate targetDate, LocalDate today) {
    if (targetDate == null) {
      return Long.MAX_VALUE;
    }
    if (!targetDate.isBefore(today)) {
      return java.time.temporal.ChronoUnit.DAYS.between(today, targetDate);
    }
    return java.time.temporal.ChronoUnit.DAYS.between(today, targetDate.plusYears(1));
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
