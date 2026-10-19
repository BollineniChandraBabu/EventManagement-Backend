package com.familywishes.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ImageContentTypesTest {

  private static byte[] bytes(int... values) {
    byte[] data = new byte[values.length];
    for (int i = 0; i < values.length; i++) {
      data[i] = (byte) values[i];
    }
    return data;
  }

  @Test
  void recognisesCommonImageFormats() {
    assertEquals("image/png", ImageContentTypes.detect(bytes(0x89, 'P', 'N', 'G', 0x0D, 0x0A)));
    assertEquals("image/jpeg", ImageContentTypes.detect(bytes(0xFF, 0xD8, 0xFF, 0xE0, 0x00)));
    assertEquals("image/gif", ImageContentTypes.detect(bytes('G', 'I', 'F', '8', '9', 'a')));
    assertEquals(
        "image/webp",
        ImageContentTypes.detect(
            bytes('R', 'I', 'F', 'F', 0x10, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P', 'V', 'P')));
  }

  @Test
  void fallsBackToOctetStreamForUnknownOrMissingData() {
    assertEquals(ImageContentTypes.DEFAULT, ImageContentTypes.detect(bytes(1, 2, 3, 4, 5)));
    assertEquals(ImageContentTypes.DEFAULT, ImageContentTypes.detect(bytes(0x89, 'P')));
    assertEquals(ImageContentTypes.DEFAULT, ImageContentTypes.detect(new byte[0]));
    assertEquals(ImageContentTypes.DEFAULT, ImageContentTypes.detect(null));
  }

  @Test
  void aRiffFileThatIsNotWebpIsNotReportedAsWebp() {
    byte[] wave = bytes('R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'A', 'V', 'E');

    assertEquals(ImageContentTypes.DEFAULT, ImageContentTypes.detect(wave));
  }
}
