package com.familywishes.dto;

import java.time.LocalDateTime;

public final class WishImageDtos {
  private WishImageDtos() {}

  public record WishImageResponse(
      Long id,
      Long userId,
      String userName,
      String eventType,
      String imageUrl,
      boolean active,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {}
}
