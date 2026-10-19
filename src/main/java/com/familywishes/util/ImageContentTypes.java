package com.familywishes.util;

/** Works out the media type of an image from its first bytes. */
public final class ImageContentTypes {

  public static final String DEFAULT = "application/octet-stream";

  private ImageContentTypes() {}

  public static String detect(byte[] data) {
    if (data == null || data.length < 4) {
      return DEFAULT;
    }
    if (startsWith(data, 0x89, 'P', 'N', 'G')) {
      return "image/png";
    }
    if (startsWith(data, 0xFF, 0xD8, 0xFF)) {
      return "image/jpeg";
    }
    if (startsWith(data, 'G', 'I', 'F', '8')) {
      return "image/gif";
    }
    if (isWebp(data)) {
      return "image/webp";
    }
    return DEFAULT;
  }

  /** WebP files start with "RIFF", then a 4 byte size, then "WEBP". */
  private static boolean isWebp(byte[] data) {
    return data.length >= 12
        && startsWith(data, 'R', 'I', 'F', 'F')
        && data[8] == 'W'
        && data[9] == 'E'
        && data[10] == 'B'
        && data[11] == 'P';
  }

  private static boolean startsWith(byte[] data, int... prefix) {
    if (data.length < prefix.length) {
      return false;
    }
    for (int i = 0; i < prefix.length; i++) {
      if ((data[i] & 0xFF) != prefix[i]) {
        return false;
      }
    }
    return true;
  }
}
