package com.familywishes.service.impl;

import com.familywishes.config.CacheNames;
import com.familywishes.dto.EmailImage;
import com.familywishes.util.ImageContentTypes;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Downloads an email's image from storage and keeps recently used ones in memory, so opening the
 * same preview twice does not download it twice.
 *
 * <p>This is a separate bean (not a method of the email service) because Spring applies caching
 * through a proxy, which a call from inside the same class would bypass. It does no access
 * checks: callers must verify the user may see the email before calling {@link #load}.
 */
@Service
@RequiredArgsConstructor
public class EmailImageService {

  private final SupabaseStorageService supabaseStorageService;

  /** Returns null when the image is missing; null results are not cached. */
  @Cacheable(cacheNames = CacheNames.EMAIL_IMAGES, key = "#emailLogId", unless = "#result == null")
  public EmailImage load(Long emailLogId, String imageKey) {
    byte[] data = supabaseStorageService.downloadImage(imageKey);
    if (data == null || data.length == 0) {
      return null;
    }
    return new EmailImage(data, ImageContentTypes.detect(data));
  }

  @CacheEvict(cacheNames = CacheNames.EMAIL_IMAGES, key = "#emailLogId")
  public void evict(Long emailLogId) {
    // the annotation does the work
  }
}
