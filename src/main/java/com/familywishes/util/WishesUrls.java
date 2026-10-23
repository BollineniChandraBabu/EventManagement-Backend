package com.familywishes.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/** Validates the optional "custom wishes page" link that can be added to a user or an event. */
public final class WishesUrls {

  public static final int MAX_LENGTH = 500;

  private WishesUrls() {}

  /**
   * Returns the trimmed link, or null when none was given (the link feature is then skipped).
   *
   * @throws IllegalArgumentException with a message that is safe to show to the user
   */
  public static String normalizeOrNull(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String url = raw.trim();
    if (url.length() > MAX_LENGTH) {
      throw new IllegalArgumentException("The wishes link must be at most " + MAX_LENGTH + " characters");
    }
    if (containsWhitespaceOrControlCharacters(url)) {
      throw new IllegalArgumentException("The wishes link must not contain spaces");
    }
    URI uri = parse(url);
    String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
    if (!scheme.equals("http") && !scheme.equals("https")) {
      throw new IllegalArgumentException("The wishes link must start with http:// or https://");
    }
    if (uri.getHost() == null || uri.getHost().isBlank()) {
      throw new IllegalArgumentException("The wishes link must contain a website address");
    }
    if (uri.getUserInfo() != null) {
      throw new IllegalArgumentException("The wishes link must not contain a username or password");
    }
    return url;
  }

  private static URI parse(String url) {
    try {
      return new URI(url);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("The wishes link is not a valid web address");
    }
  }

  private static boolean containsWhitespaceOrControlCharacters(String value) {
    return value.chars().anyMatch(ch -> Character.isWhitespace(ch) || Character.isISOControl(ch));
  }
}
