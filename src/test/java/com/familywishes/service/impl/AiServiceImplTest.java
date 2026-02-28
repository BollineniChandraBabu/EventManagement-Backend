package com.familywishes.service.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.familywishes.dto.AiWishRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class AiServiceImplTest {

  @Mock private RestTemplate restTemplate;

  private AiServiceImpl aiService;

  @BeforeEach
  void setUp() {
    aiService = new AiServiceImpl(restTemplate);
    ReflectionTestUtils.setField(
        aiService, "pollinationsImageUrl", "https://gen.pollinations.ai/image/");
    ReflectionTestUtils.setField(aiService, "pollinationsApiKey", "secret-key");
  }

  @Test
  void callGeminiImageShouldReturnNullWhenPollinationsRequestFails()
      throws JsonProcessingException {
    AiWishRequest request = new AiWishRequest("John", "Family", "Birthday", "", "Warm", "EN");
    HttpHeaders headers = new HttpHeaders();

    HttpClientErrorException badRequest =
        HttpClientErrorException.create(
            HttpStatus.BAD_REQUEST,
            "Bad Request",
            headers,
            "{\"error\":\"invalid prompt\"}".getBytes(),
            null);

    when(restTemplate.exchange(
            anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class)))
        .thenThrow(badRequest);

    byte[] image = aiService.callGeminiImage(request);

    assertNull(image);
  }

  @Test
  void callGeminiImageShouldReturnImageWhenProviderRespondsSuccessfully()
      throws JsonProcessingException {
    AiWishRequest request = new AiWishRequest("John", "Family", "Birthday", "", "Warm", "EN");
    byte[] expected = new byte[] {1, 2, 3};

    when(restTemplate.exchange(
            anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class)))
        .thenReturn(ResponseEntity.ok(expected));

    byte[] image = aiService.callGeminiImage(request);

    assertArrayEquals(expected, image);
    verify(restTemplate)
        .exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class));
  }
}
