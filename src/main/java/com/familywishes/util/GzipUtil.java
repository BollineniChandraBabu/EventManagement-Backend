package com.familywishes.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** GZIP helpers for stored media. */
public final class GzipUtil {

  private static final int BUFFER_SIZE = 8192;

  private GzipUtil() {}

  /**
   * Compresses the bytes. Returns the input unchanged when it is null, empty or already gzipped.
   */
  public static byte[] compress(byte[] data) {
    if (data == null || data.length == 0 || isGzipped(data)) {
      return data;
    }
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(32, data.length / 2));
      try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
        gzip.write(data);
      }
      return out.toByteArray();
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to gzip the data", ex);
    }
  }

  /**
   * Expands gzipped bytes. Non-gzipped input is returned unchanged, so it is safe to call twice.
   */
  public static byte[] decompress(byte[] data) {
    if (!isGzipped(data)) {
      return data;
    }
    try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(data))) {
      ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(32, data.length * 2));
      byte[] buffer = new byte[BUFFER_SIZE];
      int read;
      while ((read = gzip.read(buffer)) != -1) {
        out.write(buffer, 0, read);
      }
      return out.toByteArray();
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to gunzip the data", ex);
    }
  }

  /** True when the bytes start with the gzip magic number (0x1f 0x8b). */
  public static boolean isGzipped(byte[] data) {
    return data != null && data.length >= 2 && data[0] == (byte) 0x1F && data[1] == (byte) 0x8B;
  }
}
