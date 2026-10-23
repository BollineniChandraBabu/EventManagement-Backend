package com.familywishes.service.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.familywishes.entity.User;
import com.familywishes.entity.WishImage;
import com.familywishes.exception.BadRequestException;
import com.familywishes.repository.UserRepository;
import com.familywishes.repository.WishImageRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class WishImageServiceImplTest {

  private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3, 4};

  @Mock private WishImageRepository wishImageRepository;
  @Mock private UserRepository userRepository;
  @Mock private SupabaseStorageService storageService;

  @InjectMocks private WishImageServiceImpl service;

  private static WishImage image(String eventType, String objectKey, User user) {
    WishImage image = new WishImage();
    image.setEventType(eventType);
    image.setObjectKey(objectKey);
    image.setUser(user);
    return image;
  }

  @Test
  void aFestivalImageBeatsTheGeneralFestivalImage() {
    when(wishImageRepository.findByEventTypeIgnoreCaseAndActiveTrueAndUser_Id("Diwali", 7L))
        .thenReturn(List.of());
    when(wishImageRepository.findByEventTypeIgnoreCaseAndActiveTrueAndUserIsNull("Diwali"))
        .thenReturn(List.of(image("Diwali", "diwali", null)));
    when(storageService.downloadImage("diwali")).thenReturn(new byte[] {1});

    byte[] result = service.findRandomImage(7L, List.of("Diwali", "Festival"));

    assertArrayEquals(new byte[] {1}, result);
    verify(wishImageRepository, never())
        .findByEventTypeIgnoreCaseAndActiveTrueAndUserIsNull("Festival");
  }

  @Test
  void anImageMadeForTheUserBeatsTheSharedOne() {
    User asha = User.builder().id(7L).build();
    when(wishImageRepository.findByEventTypeIgnoreCaseAndActiveTrueAndUser_Id("Birthday", 7L))
        .thenReturn(List.of(image("Birthday", "mine", asha)));
    when(storageService.downloadImage("mine")).thenReturn(new byte[] {4});

    assertArrayEquals(new byte[] {4}, service.findRandomImage(7L, List.of("Birthday")));
    verify(wishImageRepository, never()).findByEventTypeIgnoreCaseAndActiveTrueAndUserIsNull(any());
  }

  @Test
  void aFileMissingInStorageIsSkippedSoTheNextImageIsUsed() {
    when(wishImageRepository.findByEventTypeIgnoreCaseAndActiveTrueAndUser_Id("Birthday", 7L))
        .thenReturn(List.of(image("Birthday", "gone", null)));
    when(wishImageRepository.findByEventTypeIgnoreCaseAndActiveTrueAndUserIsNull("Birthday"))
        .thenReturn(List.of(image("Birthday", "shared", null)));
    when(storageService.downloadImage("gone")).thenReturn(null);
    when(storageService.downloadImage("shared")).thenReturn(new byte[] {5});

    assertArrayEquals(new byte[] {5}, service.findRandomImage(7L, List.of("Birthday")));
  }

  @Test
  void withoutAnyUploadedImageNullIsReturnedSoTheCallerGeneratesOne() {
    when(wishImageRepository.findByEventTypeIgnoreCaseAndActiveTrueAndUserIsNull("Holi"))
        .thenReturn(List.of());

    assertNull(service.findRandomImage(null, List.of("Holi", "  ")));
  }

  @Test
  void onlyRealPngJpegGifAndWebpImagesAreAccepted() {
    assertThrows(
        BadRequestException.class,
        () ->
            service.create(
                null,
                "Birthday",
                true,
                new MockMultipartFile("file", "x.svg", "image/svg+xml", "<svg onload=alert(1)/>".getBytes())));
    // an SVG renamed to PNG is caught by looking at the bytes
    assertThrows(
        BadRequestException.class,
        () ->
            service.create(
                null, "Birthday", true, new MockMultipartFile("file", "x.png", "image/png", "<svg/>".getBytes())));
    // a real PNG must not be stored under a script-capable content type
    assertThrows(
        BadRequestException.class,
        () -> service.create(null, "Birthday", true, new MockMultipartFile("file", "x.png", "text/html", PNG)));
  }

  @Test
  void imagesOverFiveMegabytesAndLongEventTypesAreRejected() {
    byte[] huge = new byte[5 * 1024 * 1024 + 1];
    System.arraycopy(PNG, 0, huge, 0, PNG.length);

    assertThrows(
        BadRequestException.class,
        () -> service.create(null, "Birthday", true, new MockMultipartFile("file", "x.png", "image/png", huge)));
    assertThrows(
        BadRequestException.class,
        () ->
            service.create(
                null, "x".repeat(151), true, new MockMultipartFile("file", "x.png", "image/png", PNG)));
  }
}
