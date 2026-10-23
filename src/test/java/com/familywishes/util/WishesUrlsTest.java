package com.familywishes.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class WishesUrlsTest {

  @Test
  void blankMeansNoLinkSoTheFeatureIsSkipped() {
    assertNull(WishesUrls.normalizeOrNull(null));
    assertNull(WishesUrls.normalizeOrNull(""));
    assertNull(WishesUrls.normalizeOrNull("   "));
  }

  @Test
  void acceptsHttpAndHttpsLinksAndTrimsThem() {
    assertEquals(
        "https://example.com/birthday/asha.html",
        WishesUrls.normalizeOrNull("  https://example.com/birthday/asha.html  "));
    assertEquals("http://example.com", WishesUrls.normalizeOrNull("http://example.com"));
    assertEquals(
        "HTTPS://Example.com/a?b=1&c=2#top",
        WishesUrls.normalizeOrNull("HTTPS://Example.com/a?b=1&c=2#top"));
  }

  @Test
  void rejectsLinksThatAreNotWebAddresses() {
    for (String bad :
        new String[] {
          "javascript:alert(1)",
          "data:text/html;base64,PHNjcmlwdD4=",
          "ftp://example.com/file",
          "example.com/page",
          "//example.com/page",
          "https://",
          "https:///path-only",
          "mailto:someone@example.com"
        }) {
      assertThrows(IllegalArgumentException.class, () -> WishesUrls.normalizeOrNull(bad), bad);
    }
  }

  @Test
  void rejectsSpacesQuotesInjectionAndCredentials() {
    for (String bad :
        new String[] {
          "https://example.com/my page",
          "https://example.com/\"onmouseover=alert(1)",
          "https://example.com/<script>",
          "https://user:secret@example.com/",
          "https://example.com/\nSet-Cookie: x=1"
        }) {
      assertThrows(IllegalArgumentException.class, () -> WishesUrls.normalizeOrNull(bad), bad);
    }
  }

  @Test
  void rejectsOverlongLinks() {
    String tooLong = "https://example.com/" + "a".repeat(WishesUrls.MAX_LENGTH);

    assertThrows(IllegalArgumentException.class, () -> WishesUrls.normalizeOrNull(tooLong));
  }
}
