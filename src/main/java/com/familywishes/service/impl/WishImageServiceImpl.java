package com.familywishes.service.impl;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.WishImageDtos.WishImageResponse;
import com.familywishes.entity.User;
import com.familywishes.entity.WishImage;
import com.familywishes.exception.BadRequestException;
import com.familywishes.exception.NotFoundException;
import com.familywishes.repository.UserRepository;
import com.familywishes.repository.WishImageRepository;
import com.familywishes.service.WishImageService;
import com.familywishes.util.MediaValidation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class WishImageServiceImpl implements WishImageService {
  private final WishImageRepository wishImageRepository;
  private final UserRepository userRepository;
  private static final int MAX_EVENT_TYPE_LENGTH = 150; // size of the event_type column

  private final SupabaseStorageService storageService;

  @Override
  public WishImageResponse create(
      Long userId, String eventType, boolean active, MultipartFile file) {
    validateImage(file);
    WishImage image = new WishImage();
    image.setUser(resolveUser(userId));
    image.setEventType(normalizeEvent(eventType));
    image.setActive(active);
    image.setObjectKey(upload(file));
    return toResponse(wishImageRepository.save(image));
  }

  @Override
  public WishImageResponse update(
      Long id,
      Long userId,
      boolean clearUser,
      String eventType,
      Boolean active,
      MultipartFile file) {
    WishImage image = get(id);
    if (clearUser && userId != null) {
      throw new BadRequestException("Specify either userId or clearUser, not both");
    }
    if (clearUser) {
      image.setUser(null);
    }
    if (userId != null) {
      image.setUser(resolveUser(userId));
    }
    if (StringUtils.hasText(eventType)) {
      image.setEventType(normalizeEvent(eventType));
    }
    if (active != null) {
      image.setActive(active);
    }
    if (file != null && !file.isEmpty()) {
      validateImage(file);
      String oldObjectKey = image.getObjectKey();
      image.setObjectKey(upload(file));
      storageService.deleteObject(oldObjectKey);
    }
    return toResponse(wishImageRepository.save(image));
  }

  @Override
  public PagedResponse<WishImageResponse> list(
      int page,
      int size,
      String searchKey,
      String eventType,
      Long userId,
      Boolean active,
      String sortBy,
      String sortDir) {
    List<String> eventTypes = StringUtils.hasText(eventType) ? List.of(eventType.trim()) : null;
    List<Long> userIds = userId != null ? List.of(userId) : null;
    return list(page, size, searchKey, eventTypes, userIds, active, sortBy, sortDir);
  }

  @Override
  public PagedResponse<WishImageResponse> list(
      int page,
      int size,
      String searchKey,
      List<String> eventTypes,
      List<Long> userIds,
      Boolean active,
      String sortBy,
      String sortDir) {
    String field =
        switch (sortBy) {
          case "id", "eventType", "active", "createdAt", "updatedAt" -> sortBy;
          default -> "id";
        };
    Sort sort =
        "asc".equalsIgnoreCase(sortDir) ? Sort.by(field).ascending() : Sort.by(field).descending();

    List<String> cleanTypes =
        (eventTypes == null || eventTypes.isEmpty())
            ? null
            : eventTypes.stream().map(String::toLowerCase).toList();
    List<Long> cleanUserIds = (userIds == null || userIds.isEmpty()) ? null : userIds;

    Page<WishImage> result =
        wishImageRepository.findAllByFilters(
            searchKey == null ? "" : searchKey.trim(),
            cleanTypes,
            cleanUserIds,
            WishImage.toActiveFilter(active),
            PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), sort));
    return new PagedResponse<>(
        result.getContent().stream().map(this::toResponse).toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages(),
        result.hasNext(),
        result.hasPrevious());
  }

  @Override
  public void delete(Long id) {
    WishImage image =
        wishImageRepository
            .findById(id)
            .orElseThrow(
                () -> new com.familywishes.exception.NotFoundException("Wish image not found"));
    wishImageRepository.delete(image);
    storageService.deleteObject(image.getObjectKey());
  }

  @Override
  public void deleteByIds(java.util.List<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      throw new com.familywishes.exception.BadRequestException(
          "At least one wish image id is required");
    }
    ids.stream().distinct().forEach(this::delete);
  }

  @Override
  public byte[] findRandomImage(String eventType, Long userId) {
    return findRandomImage(userId, List.of(eventType == null ? "" : eventType));
  }

  @Override
  public byte[] findRandomImage(Long userId, List<String> eventTypes) {
    for (String eventType : eventTypes) {
      if (!StringUtils.hasText(eventType)) {
        continue;
      }
      String normalizedEvent = normalizeEvent(eventType);
      byte[] image = downloadAny(userCandidates(normalizedEvent, userId));
      if (image == null) {
        image =
            downloadAny(
                wishImageRepository.findByEventTypeIgnoreCaseAndActiveTrueAndUserIsNull(
                    normalizedEvent));
      }
      if (image != null) {
        return image;
      }
    }
    return null;
  }

  private List<WishImage> userCandidates(String eventType, Long userId) {
    return userId == null
        ? List.of()
        : wishImageRepository.findByEventTypeIgnoreCaseAndActiveTrueAndUser_Id(eventType, userId);
  }

  /**
   * Picks images in random order until one can be downloaded (a file may be missing in storage).
   */
  private byte[] downloadAny(List<WishImage> candidates) {
    List<WishImage> shuffled = new ArrayList<>(candidates);
    Collections.shuffle(shuffled, ThreadLocalRandom.current());
    for (WishImage candidate : shuffled) {
      byte[] data = storageService.downloadImage(candidate.getObjectKey());
      if (data != null && data.length > 0) {
        return data;
      }
      log.warn("Wish image {} could not be downloaded, trying another one", candidate.getId());
    }
    return null;
  }

  private WishImage get(Long id) {
    return wishImageRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Wish image not found"));
  }

  private User resolveUser(Long userId) {
    return userId == null
        ? null
        : userRepository
            .findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));
  }

  private String normalizeEvent(String value) {
    if (!StringUtils.hasText(value)) {
      throw new BadRequestException("Event type is required");
    }
    String eventType = value.trim();
    if (eventType.length() > MAX_EVENT_TYPE_LENGTH) {
      throw new BadRequestException(
          "Event type must be at most " + MAX_EVENT_TYPE_LENGTH + " characters");
    }
    return eventType;
  }

  /** A real image, video, audio clip or PDF within the size limit for its type. */
  private void validateImage(MultipartFile file) {
    MediaValidation.validate(file);
  }

  private String upload(MultipartFile file) {
    String objectKey = storageService.uploadWishImage(file);
    if (!StringUtils.hasText(objectKey)) {
      throw new BadRequestException("Unable to upload image to storage");
    }
    return objectKey;
  }

  private WishImageResponse toResponse(WishImage image) {
    User user = image.getUser();
    return new WishImageResponse(
        image.getId(),
        user == null ? null : user.getId(),
        user == null ? null : user.getName(),
        image.getEventType(),
        storageService.getSignedUrl(image.getObjectKey()),
        image.isActive(),
        image.getCreatedAt(),
        image.getUpdatedAt());
  }
}
