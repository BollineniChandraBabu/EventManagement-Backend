package com.familywishes.service;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.WishImageDtos.WishImageResponse;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface WishImageService {
  WishImageResponse create(Long userId, String eventType, boolean active, MultipartFile file);

  WishImageResponse update(
      Long id, Long userId, boolean clearUser, String eventType, Boolean active, MultipartFile file);

  PagedResponse<WishImageResponse> list(
      int page, int size, String searchKey, String eventType, Long userId, Boolean active, String sortBy, String sortDir);

  void delete(Long id);

  byte[] findRandomImage(String eventType, Long userId);

  /**
   * Looks for an uploaded image for each event type in order (most specific first). For each one,
   * images made for this user win over images made for everyone. Returns null when there is none,
   * in which case the caller generates an image instead.
   */
  byte[] findRandomImage(Long userId, List<String> eventTypes);
}
