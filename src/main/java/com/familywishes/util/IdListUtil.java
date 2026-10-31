package com.familywishes.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class IdListUtil {
  private IdListUtil() {}

  public static List<Long> parseIds(String idString, List<Long> requestIds) {
    if (requestIds != null && !requestIds.isEmpty()) {
      return requestIds.stream().filter(Objects::nonNull).distinct().toList();
    }
    if (idString == null || idString.isBlank()) {
      return Collections.emptyList();
    }
    return Arrays.stream(idString.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(Long::parseLong)
        .distinct()
        .toList();
  }
}
