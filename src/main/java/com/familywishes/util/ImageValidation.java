package com.familywishes.util;

import com.familywishes.exception.BadRequestException;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import org.springframework.web.multipart.MultipartFile;

/** Validates an uploaded greeting image: a real PNG/JPEG/GIF/WebP up to 5 MB. */
public final class ImageValidation {

  public static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;

  private static final Set<String> ALLOWED_IMAGE_TYPES =
      Set.of("image/png", "image/jpeg", "image/gif", "image/webp");

  private ImageValidation() {}

  /** Only real images are accepted; an SVG (or a file renamed to .png) is rejected. */
  public static void validate(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BadRequestException("A non-empty image file is required");
    }
    if (file.getSize() > MAX_IMAGE_BYTES) {
      throw new BadRequestException("The image must be 5 MB or smaller");
    }
    String declaredType =
        file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
    if (!ALLOWED_IMAGE_TYPES.contains(declaredType)
        || !ALLOWED_IMAGE_TYPES.contains(detectType(file))) {
      throw new BadRequestException("Only PNG, JPEG, GIF or WebP images are allowed");
    }
  }

  /** Looks at the file's first bytes, so a renamed file cannot pass as an image. */
  private static String detectType(MultipartFile file) {
    try {
      return ImageContentTypes.detect(file.getBytes());
    } catch (IOException e) {
      throw new BadRequestException("The image could not be read");
    }
  }
}
