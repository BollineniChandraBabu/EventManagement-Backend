package com.familywishes.dto;

import com.familywishes.entity.enums.CircleStatus;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public final class CircleDtos {
  private CircleDtos() {}

  /** One side of a circle as seen by the current user. */
  public record CircleResponse(
      Long circleId,
      Long userId,
      String name,
      String email,
      String profilePictureUrl,
      boolean online,
      LocalDateTime lastSeenAt) {}

  /** A pending request together with the other user and which way it points. */
  public record CircleRequestResponse(
      Long circleId,
      Long userId,
      String name,
      String email,
      String profilePictureUrl,
      boolean incoming,
      CircleStatus status,
      LocalDateTime createdAt) {}

  /** The relationship between the current user and one other user. */
  public record CircleStatusResponse(
      Long userId, CircleStatus status, boolean inCircle, Long circleId) {}

  public record SendCircleRequest(@NotNull Long userId) {}

  public record CircleRequestAction(@NotNull Long circleId) {}

  public record CircleListResponse(
      java.util.List<CircleResponse> circles,
      java.util.List<CircleRequestResponse> incomingRequests,
      java.util.List<CircleRequestResponse> outgoingRequests) {}
}
