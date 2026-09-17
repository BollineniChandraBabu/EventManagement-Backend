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
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class WishImageServiceImpl implements WishImageService {
  private final WishImageRepository wishImageRepository;
  private final UserRepository userRepository;
  private final SupabaseStorageService storageService;

  @Override
  public WishImageResponse create(Long userId, String eventType, boolean active, MultipartFile file) {
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
    String field =
        switch (sortBy) {
          case "id", "eventType", "active", "createdAt", "updatedAt" -> sortBy;
          default -> "id";
        };
    Sort sort =
        "asc".equalsIgnoreCase(sortDir)
            ? Sort.by(field).ascending()
            : Sort.by(field).descending();
    var result =
        wishImageRepository.findAllFiltered(
            searchKey == null ? "" : searchKey.trim(),
            eventType == null ? "" : eventType.trim(),
            userId,
            active,
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
    WishImage image = get(id);
    wishImageRepository.delete(image);
    storageService.deleteObject(image.getObjectKey());
  }

  @Override
  public byte[] findRandomImage(String eventType, Long userId) {
    if (!StringUtils.hasText(eventType)) {
      return null;
    }
    String normalizedEvent = normalizeEvent(eventType);
    List<WishImage> candidates =
        userId == null
            ? List.of()
            : wishImageRepository.findByEventTypeIgnoreCaseAndActiveTrueAndUser_Id(
                normalizedEvent, userId);
    if (candidates.isEmpty()) {
      candidates = wishImageRepository.findByEventTypeIgnoreCaseAndActiveTrueAndUserIsNull(normalizedEvent);
    }
    if (candidates.isEmpty()) {
      return null;
    }
    WishImage selected = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    return storageService.downloadImage(selected.getObjectKey());
  }

  private WishImage get(Long id) {
    return wishImageRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Wish image not found"));
  }

  private User resolveUser(Long userId) {
    return userId == null
        ? null
        : userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
  }

  private String normalizeEvent(String value) {
    if (!StringUtils.hasText(value)) {
      throw new BadRequestException("Event type is required");
    }
    return value.trim();
  }

  private void validateImage(MultipartFile file) {
    if (file == null
        || file.isEmpty()
        || file.getContentType() == null
        || !file.getContentType().startsWith("image/")) {
      throw new BadRequestException("A non-empty image file is required");
    }
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
        storageService.getPublicUrl(image.getObjectKey()),
        image.isActive(),
        image.getCreatedAt(),
        image.getUpdatedAt());
  }
}
