package com.familywishes.util;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

/** Works out the media type of an uploaded file from its first bytes. */
public final class MediaContentTypes {

  public static final String DEFAULT = "application/octet-stream";

  // Images
  public static final String PNG = "image/png";
  public static final String JPEG = "image/jpeg";
  public static final String GIF = "image/gif";
  public static final String WEBP = "image/webp";
  public static final String BMP = "image/bmp";

  // Videos
  public static final String MP4 = "video/mp4";
  public static final String WEBM = "video/webm";
  public static final String QUICKTIME = "video/quicktime";
  public static final String AVI = "video/x-msvideo";
  public static final String MATROSKA = "video/x-matroska";
  public static final String MPEG = "video/mpeg";
  public static final String VIDEO_3GPP = "video/3gpp";

  // Audio
  public static final String MP3 = "audio/mpeg";
  public static final String WAV = "audio/wav";
  public static final String OGG = "audio/ogg";
  public static final String M4A = "audio/mp4";
  public static final String FLAC = "audio/flac";

  // Documents
  public static final String PDF = "application/pdf";

  private static final Map<String, String> EXTENSIONS =
      Map.ofEntries(
          Map.entry(PNG, ".png"),
          Map.entry(JPEG, ".jpg"),
          Map.entry(GIF, ".gif"),
          Map.entry(WEBP, ".webp"),
          Map.entry(BMP, ".bmp"),
          Map.entry(MP4, ".mp4"),
          Map.entry(WEBM, ".webm"),
          Map.entry(QUICKTIME, ".mov"),
          Map.entry(AVI, ".avi"),
          Map.entry(MATROSKA, ".mkv"),
          Map.entry(MPEG, ".mpeg"),
          Map.entry(VIDEO_3GPP, ".3gp"),
          Map.entry(MP3, ".mp3"),
          Map.entry(WAV, ".wav"),
          Map.entry(OGG, ".ogg"),
          Map.entry(M4A, ".m4a"),
          Map.entry(FLAC, ".flac"),
          Map.entry(PDF, ".pdf"));

  private MediaContentTypes() {}

  public static String detect(byte[] data) {
    if (data == null || data.length < 4) {
      return DEFAULT;
    }
    if (startsWith(data, 0x89, 'P', 'N', 'G')) {
      return PNG;
    }
    if (startsWith(data, 0xFF, 0xD8, 0xFF)) {
      return JPEG;
    }
    if (startsWith(data, 'G', 'I', 'F', '8')) {
      return GIF;
    }
    if (isWebp(data)) {
      return WEBP;
    }
    if (startsWith(data, 'B', 'M')) {
      return BMP;
    }
    if (startsWith(data, '%', 'P', 'D', 'F')) {
      return PDF;
    }
    if (startsWith(data, 'f', 'L', 'a', 'C')) {
      return FLAC;
    }
    if (startsWith(data, 'O', 'g', 'g', 'S')) {
      return OGG;
    }
    if (startsWith(data, 'I', 'D', '3') || isMpegAudio(data)) {
      return MP3;
    }
    if (isIsoBmff(data)) {
      return isoBmffType(data);
    }
    if (isEbml(data)) {
      return isMatroskaDocType(data, "webm") ? WEBM : MATROSKA;
    }
    if (isRiff(data)) {
      return riffType(data);
    }
    if (isMpegVideo(data)) {
      return MPEG;
    }
    return DEFAULT;
  }

  public static boolean isImage(String contentType) {
    return contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("image/");
  }

  public static boolean isImage(byte[] data) {
    return isImage(detect(data));
  }

  public static boolean isVideo(String contentType) {
    return contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("video/");
  }

  public static boolean isAudio(String contentType) {
    return contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("audio/");
  }

  public static String fileExtension(String contentType) {
    return EXTENSIONS.getOrDefault(contentType, ".bin");
  }

  /** WebP and AVI both start with "RIFF", then a 4 byte size, then a 4 byte kind. */
  private static boolean isRiff(byte[] data) {
    return data.length >= 12 && startsWith(data, 'R', 'I', 'F', 'F');
  }

  private static String riffType(byte[] data) {
    if (startsWithAt(data, 8, 'W', 'E', 'B', 'P')) {
      return WEBP;
    }
    if (startsWithAt(data, 8, 'A', 'V', 'I', ' ')) {
      return AVI;
    }
    if (startsWithAt(data, 8, 'W', 'A', 'V', 'E')) {
      return WAV;
    }
    return DEFAULT;
  }

  private static boolean isWebp(byte[] data) {
    return isRiff(data) && startsWithAt(data, 8, 'W', 'E', 'B', 'P');
  }

  /** MP4, MOV, M4A and 3GP all carry an ISO base media "ftyp" box at offset 4. */
  private static boolean isIsoBmff(byte[] data) {
    return data.length >= 12 && startsWithAt(data, 4, 'f', 't', 'y', 'p');
  }

  private static String isoBmffType(byte[] data) {
    String brand = new String(data, 8, 4, StandardCharsets.ISO_8859_1);
    if (brand.startsWith("qt")) {
      return QUICKTIME;
    }
    if (brand.startsWith("M4A") || brand.startsWith("M4B") || brand.startsWith("M4P")) {
      return M4A;
    }
    if (brand.startsWith("3gp")) {
      return VIDEO_3GPP;
    }
    return MP4;
  }

  /** Matroska and WebM share the EBML header; the DocType tells them apart. */
  private static boolean isEbml(byte[] data) {
    return startsWith(data, 0x1A, 0x45, 0xDF, 0xA3);
  }

  private static boolean isMatroskaDocType(byte[] data, String docType) {
    byte[] needle = docType.getBytes(StandardCharsets.ISO_8859_1);
    int limit = Math.min(data.length, 64) - needle.length;
    for (int i = 0; i <= limit; i++) {
      boolean match = true;
      for (int j = 0; j < needle.length; j++) {
        if (data[i + j] != needle[j]) {
          match = false;
          break;
        }
      }
      if (match) {
        return true;
      }
    }
    return false;
  }

  /** MP3 without an ID3 tag starts with a frame sync (0xFFEx). */
  private static boolean isMpegAudio(byte[] data) {
    return data.length >= 2 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xE0) == 0xE0;
  }

  private static boolean isMpegVideo(byte[] data) {
    return startsWith(data, 0x00, 0x00, 0x01, 0xBA) || startsWith(data, 0x00, 0x00, 0x01, 0xB3);
  }

  private static boolean startsWith(byte[] data, int... prefix) {
    return startsWithAt(data, 0, prefix);
  }

  private static boolean startsWithAt(byte[] data, int offset, int... prefix) {
    if (data.length < offset + prefix.length) {
      return false;
    }
    for (int i = 0; i < prefix.length; i++) {
      if ((data[offset + i] & 0xFF) != prefix[i]) {
        return false;
      }
    }
    return true;
  }
}
