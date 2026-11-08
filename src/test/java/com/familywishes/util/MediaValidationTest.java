package com.familywishes.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.familywishes.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class MediaValidationTest {

  private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 1, 2, 3};
  private static final byte[] PDF = {'%', 'P', 'D', 'F', '-', '1', '.', '7'};
  private static final byte[] MP4 = {0, 0, 0, 0x20, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm'};
  private static final byte[] SVG = "<svg xmlns='http://www.w3.org/2000/svg'></svg>".getBytes();

  private static MockMultipartFile file(String name, String contentType, byte[] data) {
    return new MockMultipartFile("file", name, contentType, data);
  }

  @Test
  void acceptsAnImageWithAMatchingDeclaredType() {
    MediaValidation.validate(file("photo.png", "image/png", PNG));
    assertEquals(
        "image/png", MediaValidation.validateAndDetect(file("photo.png", "image/png", PNG)));
  }

  @Test
  void acceptsMediaWhenTheDeclaredTypeIsMissingOrGeneric() {
    MediaValidation.validate(file("photo.png", null, PNG));
    MediaValidation.validate(file("photo.png", "application/octet-stream", PNG));
  }

  @Test
  void acceptsVideoAudioAndPdf() {
    MediaValidation.validate(file("clip.mp4", "video/mp4", MP4));
    MediaValidation.validate(file("doc.pdf", "application/pdf", PDF));
    MediaValidation.validate(file("doc.pdf", null, PDF));
  }

  @Test
  void rejectsAnEmptyFile() {
    assertThrows(BadRequestException.class, () -> MediaValidation.validate(null));
    assertThrows(
        BadRequestException.class,
        () -> MediaValidation.validate(file("x.png", "image/png", new byte[0])));
  }

  @Test
  void rejectsAnUnsupportedType() {
    assertThrows(
        BadRequestException.class,
        () -> MediaValidation.validate(file("evil.svg", "image/svg+xml", SVG)));
  }

  @Test
  void rejectsARenamedFileWhoseBytesDoNotMatch() {
    // A PDF renamed as a PNG must not pass as an image.
    assertThrows(
        BadRequestException.class,
        () -> MediaValidation.validate(file("fake.png", "image/png", PDF)));
  }

  @Test
  void rejectsADeclaredTypeThatDisagreesWithTheBytes() {
    // Real PNG bytes declared as a PDF.
    assertThrows(
        BadRequestException.class,
        () -> MediaValidation.validate(file("mismatch.pdf", "application/pdf", PNG)));
  }

  @Test
  void imageOnlyValidationRejectsNonImages() {
    MediaValidation.validateImage(file("photo.png", "image/png", PNG));
    assertThrows(
        BadRequestException.class,
        () -> MediaValidation.validateImage(file("doc.pdf", "application/pdf", PDF)));
  }

  @Test
  void rejectsFilesOverTheSizeLimitForTheirKind() {
    byte[] huge = new byte[(int) MediaValidation.MAX_IMAGE_BYTES + 1];
    System.arraycopy(PNG, 0, huge, 0, PNG.length);
    assertThrows(
        BadRequestException.class,
        () -> MediaValidation.validate(file("big.png", "image/png", huge)));
  }

  @Test
  void attachmentValidationAcceptsAnyFileAndDetectsItsType() {
    // Chat attachments may be any file, not only media; the bytes still decide the type.
    assertEquals(
        "image/png",
        MediaValidation.validateAttachmentAndDetect(file("photo.png", "image/png", PNG)));
    assertEquals(
        "application/octet-stream",
        MediaValidation.validateAttachmentAndDetect(
            file("notes.txt", "text/plain", new byte[] {1, 2, 3, 4})));
  }

  @Test
  void attachmentValidationRejectsEmptyFiles() {
    assertThrows(
        BadRequestException.class,
        () ->
            MediaValidation.validateAttachmentAndDetect(file("x.txt", "text/plain", new byte[0])));
  }
}
