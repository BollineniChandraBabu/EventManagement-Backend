package com.familywishes.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MediaContentTypesTest {

  private static byte[] bytes(int... values) {
    byte[] out = new byte[values.length];
    for (int i = 0; i < values.length; i++) {
      out[i] = (byte) values[i];
    }
    return out;
  }

  private static byte[] concat(byte[] head, String tail) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    out.writeBytes(head);
    out.writeBytes(tail.getBytes(StandardCharsets.ISO_8859_1));
    return out.toByteArray();
  }

  @Test
  void detectsImages() {
    assertEquals(
        MediaContentTypes.PNG, MediaContentTypes.detect(bytes(0x89, 'P', 'N', 'G', 0x0D, 0x0A)));
    assertEquals(
        MediaContentTypes.JPEG, MediaContentTypes.detect(bytes(0xFF, 0xD8, 0xFF, 0xE0, 0x00)));
    assertEquals(
        MediaContentTypes.GIF, MediaContentTypes.detect(bytes('G', 'I', 'F', '8', '9', 'a')));
    assertEquals(
        MediaContentTypes.BMP, MediaContentTypes.detect(bytes('B', 'M', 0x00, 0x00, 0x00)));
  }

  @Test
  void detectsWebpFromItsRiffHeader() {
    byte[] webp = concat(bytes('R', 'I', 'F', 'F', 0, 0, 0, 0), "WEBPVP8 ");
    assertEquals(MediaContentTypes.WEBP, MediaContentTypes.detect(webp));
  }

  @Test
  void detectsPdf() {
    assertEquals(MediaContentTypes.PDF, MediaContentTypes.detect(bytes('%', 'P', 'D', 'F', '-')));
  }

  @Test
  void detectsMp4AndQuicktimeFromTheFtypBrand() {
    byte[] mp4 = concat(bytes(0, 0, 0, 0x20), "ftypisom");
    assertEquals(MediaContentTypes.MP4, MediaContentTypes.detect(mp4));

    byte[] mov = concat(bytes(0, 0, 0, 0x20), "ftypqt  ");
    assertEquals(MediaContentTypes.QUICKTIME, MediaContentTypes.detect(mov));
  }

  @Test
  void detectsWebmAndMatroskaFromTheEbmlDocType() {
    byte[] webm = concat(bytes(0x1A, 0x45, 0xDF, 0xA3), "....webm....");
    assertEquals(MediaContentTypes.WEBM, MediaContentTypes.detect(webm));

    byte[] mkv = concat(bytes(0x1A, 0x45, 0xDF, 0xA3), "....matroska");
    assertEquals(MediaContentTypes.MATROSKA, MediaContentTypes.detect(mkv));
  }

  @Test
  void detectsAviAndWavFromTheirRiffHeaders() {
    byte[] avi = concat(bytes('R', 'I', 'F', 'F', 0, 0, 0, 0), "AVI LIST");
    assertEquals(MediaContentTypes.AVI, MediaContentTypes.detect(avi));

    byte[] wav = concat(bytes('R', 'I', 'F', 'F', 0, 0, 0, 0), "WAVEfmt ");
    assertEquals(MediaContentTypes.WAV, MediaContentTypes.detect(wav));
  }

  @Test
  void detectsAudioFormats() {
    assertEquals(MediaContentTypes.MP3, MediaContentTypes.detect(bytes('I', 'D', '3', 0x03)));
    assertEquals(MediaContentTypes.OGG, MediaContentTypes.detect(bytes('O', 'g', 'g', 'S', 0x00)));
    assertEquals(MediaContentTypes.FLAC, MediaContentTypes.detect(bytes('f', 'L', 'a', 'C', 0x00)));
  }

  @Test
  void detectsMpegVideo() {
    assertEquals(
        MediaContentTypes.MPEG, MediaContentTypes.detect(bytes(0x00, 0x00, 0x01, 0xBA, 0x44)));
  }

  @Test
  void fallsBackToOctetStreamWhenUnknownOrTooShort() {
    assertEquals(MediaContentTypes.DEFAULT, MediaContentTypes.detect(bytes(1, 2, 3, 4, 5)));
    assertEquals(MediaContentTypes.DEFAULT, MediaContentTypes.detect(bytes(0x89, 'P')));
    assertEquals(MediaContentTypes.DEFAULT, MediaContentTypes.detect(new byte[0]));
    assertEquals(MediaContentTypes.DEFAULT, MediaContentTypes.detect(null));
  }

  @Test
  void reportsMediaKind() {
    assertTrue(MediaContentTypes.isImage(MediaContentTypes.PNG));
    assertTrue(MediaContentTypes.isVideo(MediaContentTypes.MP4));
    assertTrue(MediaContentTypes.isAudio(MediaContentTypes.MP3));
    assertFalse(MediaContentTypes.isImage(MediaContentTypes.PDF));
    assertFalse(MediaContentTypes.isVideo(MediaContentTypes.DEFAULT));
  }

  @Test
  void mapsContentTypesToExtensions() {
    assertEquals(".png", MediaContentTypes.fileExtension(MediaContentTypes.PNG));
    assertEquals(".mp4", MediaContentTypes.fileExtension(MediaContentTypes.MP4));
    assertEquals(".pdf", MediaContentTypes.fileExtension(MediaContentTypes.PDF));
    assertEquals(".bin", MediaContentTypes.fileExtension(MediaContentTypes.DEFAULT));
  }
}
