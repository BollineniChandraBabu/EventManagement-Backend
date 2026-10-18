package com.familywishes.ai;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base URL and model taken from {@code app.gemini.api.url}, for example {@code
 * https://host/v1beta/models/<model>:generateContent}.
 */
record GeminiEndpoint(String baseUrl, String model) {

  private static final Pattern URL_PATTERN = Pattern.compile("^(.+?)/models/([^:?/]+)");

  static GeminiEndpoint parse(String apiUrl) {
    Matcher matcher = URL_PATTERN.matcher(apiUrl.trim());
    if (!matcher.find()) {
      throw new IllegalStateException(
          "app.gemini.api.url must look like https://<host>/v1beta/models/<model>:generateContent");
    }
    return new GeminiEndpoint(matcher.group(1), matcher.group(2));
  }
}
