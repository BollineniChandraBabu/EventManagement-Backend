package com.familywishes.util;

import com.familywishes.exception.BadRequestException;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import org.springframework.web.multipart.MultipartFile;

/**
 * Validates an uploaded media file: a real image, video, audio clip or PDF within its size limit.
 *
 * <p>The file's first bytes are inspected, so a renamed file (for example an SVG called ".png") is
 * rejected. The declared content type must be allowed and must belong to the same kind of media as
 * the bytes, which stops a file being stored under a misleading type.
 */
public final class MediaValidation {

  public static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
  public static final long MAX_AUDIO_BYTES = 10L * 1024 * 1024;
  public static final long MAX_DOCUMENT_BYTES = 20L * 1024 * 1024;
  public static final long MAX_VIDEO_BYTES = 50L * 1024 * 1024;
  public static final long MAX_ATTACHMENT_BYTES = 50L * 1024 * 1024;

  public static final String IMAGE_MESSAGE = "Only PNG, JPEG, GIF, WebP or BMP images are allowed";
  public static final String MEDIA_MESSAGE =
      "Only images (PNG, JPEG, GIF, WebP, BMP), videos (MP4, WebM, MOV, AVI, MKV, MPEG, 3GP), "
          + "audio (MP3, WAV, OGG, M4A, FLAC) or PDF files are allowed";

  private static final Set<String> ALLOWED_IMAGE_TYPES =
      Set.of(
          MediaContentTypes.PNG,
          MediaContentTypes.JPEG,
          MediaContentTypes.GIF,
          MediaContentTypes.WEBP,
          MediaContentTypes.BMP);

  private static final Set<String> ALLOWED_VIDEO_TYPES =
      Set.of(
          MediaContentTypes.MP4,
          MediaContentTypes.WEBM,
          MediaContentTypes.QUICKTIME,
          MediaContentTypes.AVI,
          MediaContentTypes.MATROSKA,
          MediaContentTypes.MPEG,
          MediaContentTypes.VIDEO_3GPP);

  private static final Set<String> ALLOWED_AUDIO_TYPES =
      Set.of(
          MediaContentTypes.MP3,
          MediaContentTypes.WAV,
          MediaContentTypes.OGG,
          MediaContentTypes.M4A,
          MediaContentTypes.FLAC);

  private MediaValidation() {}

  /** Accepts any supported media (image, video, audio or PDF). */
  public static void validate(MultipartFile file) {
    String detected = validateCommon(file);
    if (!isAllowed(detected)) {
      throw new BadRequestException(MEDIA_MESSAGE);
    }
  }

  /**
   * Accepts any non-empty file up to {@link #MAX_ATTACHMENT_BYTES} and returns the media type
   * worked out from its bytes. Used for chat attachments, which may be any file (archives,
   * documents, ...) and not only media.
   */
  public static String validateAttachmentAndDetect(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BadRequestException("A non-empty file is required");
    }
    if (file.getSize() > MAX_ATTACHMENT_BYTES) {
      throw new BadRequestException(
          "The file is too large; the limit is " + (MAX_ATTACHMENT_BYTES / (1024 * 1024)) + " MB");
    }
    return detectType(file);
  }

  /** Accepts only supported images. Use for endpoints that must stay image-only. */
  public static void validateImage(MultipartFile file) {
    String detected = validateCommon(file);
    if (!ALLOWED_IMAGE_TYPES.contains(detected)) {
      throw new BadRequestException(IMAGE_MESSAGE);
    }
  }

  /**
   * Validates the file and returns the media type worked out from its bytes, so the caller can
   * store it with the right content type.
   */
  public static String validateAndDetect(MultipartFile file) {
    String detected = validateCommon(file);
    if (!isAllowed(detected)) {
      throw new BadRequestException(MEDIA_MESSAGE);
    }
    return detected;
  }

  private static String validateCommon(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BadRequestException("A non-empty file is required");
    }
    String detected = detectType(file);
    long maxBytes = maxBytesFor(detected);
    if (file.getSize() > maxBytes) {
      throw new BadRequestException(
          "The file is too large; the limit for this type is "
              + (maxBytes / (1024 * 1024))
              + " MB");
    }
    String declared =
        file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
    boolean genericDeclared = declared.isBlank() || MediaContentTypes.DEFAULT.equals(declared);
    if (!genericDeclared && (!isAllowed(declared) || kind(declared) != kind(detected))) {
      throw new BadRequestException(MEDIA_MESSAGE);
    }
    return detected;
  }

  private static boolean isAllowed(String contentType) {
    return ALLOWED_IMAGE_TYPES.contains(contentType)
        || ALLOWED_VIDEO_TYPES.contains(contentType)
        || ALLOWED_AUDIO_TYPES.contains(contentType)
        || MediaContentTypes.PDF.equals(contentType);
  }

  private static long maxBytesFor(String contentType) {
    return switch (kind(contentType)) {
      case IMAGE -> MAX_IMAGE_BYTES;
      case VIDEO -> MAX_VIDEO_BYTES;
      case AUDIO -> MAX_AUDIO_BYTES;
      case DOCUMENT -> MAX_DOCUMENT_BYTES;
      case OTHER -> MAX_DOCUMENT_BYTES;
    };
  }

  private static Kind kind(String contentType) {
    if (MediaContentTypes.isImage(contentType)) {
      return Kind.IMAGE;
    }
    if (MediaContentTypes.isVideo(contentType)) {
      return Kind.VIDEO;
    }
    if (MediaContentTypes.isAudio(contentType)) {
      return Kind.AUDIO;
    }
    if (MediaContentTypes.PDF.equals(contentType)) {
      return Kind.DOCUMENT;
    }
    return Kind.OTHER;
  }

  /** Looks at the file's first bytes, so a renamed file cannot pass as another type. */
  private static String detectType(MultipartFile file) {
    try {
      return MediaContentTypes.detect(file.getBytes());
    } catch (IOException e) {
      throw new BadRequestException("The file could not be read");
    }
  }

  private enum Kind {
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    OTHER
  }
}
