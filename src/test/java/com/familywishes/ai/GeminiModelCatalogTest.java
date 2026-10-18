package com.familywishes.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;

/** Model names here are made-up fixtures; the production code contains no model names. */
class GeminiModelCatalogTest {

  private static final String API_URL =
      "https://api.example.test/v1beta/models/primary:generateContent";

  private final FakeGeminiRestTemplate google = new FakeGeminiRestTemplate();

  private GeminiModelCatalog catalog(long cacheMinutes, String excludedPattern) {
    return new GeminiModelCatalog(google, API_URL, "key", cacheMinutes, excludedPattern);
  }

  @Test
  void keepsOnlyModelsThatCanGenerateText() {
    google.listedModels = List.of("text-a", "text-b");

    assertEquals(List.of("text-a", "text-b"), catalog(60, "").availableModels());
  }

  @Test
  void skipsExcludedKindsOfModels() {
    google.listedModels = List.of("text-a", "text-image", "text-b-preview", "text-c");

    assertEquals(List.of("text-a", "text-c"), catalog(60, "image|preview").availableModels());
  }

  @Test
  void reusesTheListUntilTheCacheExpires() {
    google.listedModels = List.of("text-a");
    GeminiModelCatalog catalog = catalog(60, "");

    catalog.availableModels();
    catalog.availableModels();

    assertEquals(1, google.listCalls);
  }

  @Test
  void picksUpNewModelsAndDropsRetiredOnesWhenRefreshed() {
    GeminiModelCatalog catalog = catalog(0, ""); // 0 minutes: ask Google every time
    google.listedModels = List.of("old-model", "stable-model");
    assertEquals(List.of("old-model", "stable-model"), catalog.availableModels());

    google.listedModels = new ArrayList<>(List.of("stable-model", "brand-new-model"));
    assertEquals(List.of("stable-model", "brand-new-model"), catalog.availableModels());
  }

  @Test
  void keepsThePreviousListWhenGoogleCannotBeReached() {
    GeminiModelCatalog catalog = catalog(0, "");
    google.listedModels = List.of("text-a");
    catalog.availableModels();

    google.listFailure = new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE);

    assertEquals(List.of("text-a"), catalog.availableModels());
  }
}
