package com.familywishes.util;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class GzipUtilTest {

  private static final byte[] SAMPLE =
      "the quick brown fox jumps over the lazy dog, again and again"
          .getBytes(StandardCharsets.UTF_8);

  @Test
  void compressThenDecompressRestoresTheOriginalBytes() {
    byte[] compressed = GzipUtil.compress(SAMPLE);

    assertNotEquals(SAMPLE.length, compressed.length);
    assertTrue(GzipUtil.isGzipped(compressed));
    assertArrayEquals(SAMPLE, GzipUtil.decompress(compressed));
  }

  @Test
  void compressUsesTheGzipMagicHeader() {
    byte[] compressed = GzipUtil.compress(SAMPLE);

    assertEquals((byte) 0x1F, compressed[0]);
    assertEquals((byte) 0x8B, compressed[1]);
  }

  @Test
  void compressIsIdempotentForAlreadyGzippedData() {
    byte[] once = GzipUtil.compress(SAMPLE);
    byte[] twice = GzipUtil.compress(once);

    assertArrayEquals(once, twice);
  }

  @Test
  void decompressLeavesPlainDataUntouched() {
    assertArrayEquals(SAMPLE, GzipUtil.decompress(SAMPLE));
  }

  @Test
  void compressHandlesNullAndEmptyInput() {
    assertNull(GzipUtil.compress(null));
    assertArrayEquals(new byte[0], GzipUtil.compress(new byte[0]));
  }

  @Test
  void decompressHandlesNullAndEmptyInput() {
    assertNull(GzipUtil.decompress(null));
    assertArrayEquals(new byte[0], GzipUtil.decompress(new byte[0]));
  }

  @Test
  void isGzippedRejectsShortAndPlainArrays() {
    assertFalse(GzipUtil.isGzipped(null));
    assertFalse(GzipUtil.isGzipped(new byte[0]));
    assertFalse(GzipUtil.isGzipped(new byte[] {(byte) 0x1F}));
    assertFalse(GzipUtil.isGzipped(SAMPLE));
  }

  @Test
  void roundTripsBinaryDataIncludingNulBytes() {
    byte[] binary = new byte[512];
    for (int i = 0; i < binary.length; i++) {
      binary[i] = (byte) (i % 256);
    }

    assertArrayEquals(binary, GzipUtil.decompress(GzipUtil.compress(binary)));
  }
}
