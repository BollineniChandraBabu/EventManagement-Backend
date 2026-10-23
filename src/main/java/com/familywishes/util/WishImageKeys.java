package com.familywishes.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Works out which image "event types" to look for when a wish is sent, most specific first.
 *
 * <p>A festival wish looks for an image named after the festival (for example "Diwali") and then
 * for a general "Festival" image. Event types such as WEDDING_ANNIVERSARY also match images that
 * an admin named "Wedding Anniversary".
 */
public final class WishImageKeys {

  public static final String FESTIVAL_CATEGORY = "Festival";

  private WishImageKeys() {}

  public static List<String> forWish(String event, String festival) {
    List<String> keys = new ArrayList<>();
    addWithVariants(keys, event);
    if (festival != null && !festival.isBlank()) {
      addWithVariants(keys, festival);
      addIfMissing(keys, FESTIVAL_CATEGORY);
    }
    return keys;
  }

  private static void addWithVariants(List<String> keys, String value) {
    if (value == null || value.isBlank()) {
      return;
    }
    String trimmed = value.trim();
    addIfMissing(keys, trimmed);
    addIfMissing(keys, trimmed.replace('_', ' '));
  }

  private static void addIfMissing(List<String> keys, String value) {
    boolean alreadyThere = keys.stream().anyMatch(existing -> existing.equalsIgnoreCase(value));
    if (!alreadyThere) {
      keys.add(value);
    }
  }
}
