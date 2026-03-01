package com.familywishes.service.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.familywishes.dto.AiWishRequest;
import com.familywishes.exception.BadRequestException;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Map;
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
    ReflectionTestUtils.setField(
        aiService, "pollinationsBalanceUrl", "https://gen.pollinations.ai/account/balance");
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

  @Test
  void getPollinationsBalanceShouldReturnBalanceWhenProviderRespondsSuccessfully() {
    Map<String, Object> expected = Map.of("credits", 50);

    when(restTemplate.exchange(
            anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
        .thenReturn(ResponseEntity.ok(expected));

    Map<String, Object> balance = aiService.getPollinationsBalance();

    assertEquals(expected, balance);
    verify(restTemplate).exchange(
        anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class));
  }

  @Test
  void getPollinationsBalanceShouldThrowBadRequestExceptionWhenProviderFails() {
    HttpHeaders headers = new HttpHeaders();

    HttpClientErrorException unauthorized =
        HttpClientErrorException.create(
            HttpStatus.UNAUTHORIZED,
            "Unauthorized",
            headers,
            "{\"error\":\"invalid token\"}".getBytes(),
            null);

    when(restTemplate.exchange(
            anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
        .thenThrow(unauthorized);

    assertThrows(BadRequestException.class, () -> aiService.getPollinationsBalance());
  }
}
