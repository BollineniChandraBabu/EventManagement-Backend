package com.familywishes.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GeminiEndpointTest {

  @Test
  void splitsTheUrlIntoBaseUrlAndModel() {
    GeminiEndpoint endpoint =
        GeminiEndpoint.parse(
            "https://api.example.test/v1beta/models/some-model:generateContent?key=");

    assertEquals("https://api.example.test/v1beta", endpoint.baseUrl());
    assertEquals("some-model", endpoint.model());
  }

  @Test
  void rejectsAUrlWithoutAModel() {
    assertThrows(
        IllegalStateException.class, () -> GeminiEndpoint.parse("https://api.example.test/v1beta"));
  }
}
