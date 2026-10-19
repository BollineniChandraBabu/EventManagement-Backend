package com.familywishes.service.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.familywishes.dto.EmailImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailImageServiceTest {

  private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A};

  @Mock private SupabaseStorageService supabaseStorageService;

  @InjectMocks private EmailImageService emailImageService;

  @Test
  void loadReturnsTheImageWithItsContentType() {
    when(supabaseStorageService.downloadImage("key-1")).thenReturn(PNG);

    EmailImage image = emailImageService.load(1L, "key-1");

    assertArrayEquals(PNG, image.data());
    assertEquals("image/png", image.contentType());
  }

  @Test
  void loadReturnsNullWhenTheImageIsMissingSoNothingIsCached() {
    when(supabaseStorageService.downloadImage("gone")).thenReturn(null);
    when(supabaseStorageService.downloadImage("empty")).thenReturn(new byte[0]);

    assertNull(emailImageService.load(1L, "gone"));
    assertNull(emailImageService.load(2L, "empty"));
  }
}
