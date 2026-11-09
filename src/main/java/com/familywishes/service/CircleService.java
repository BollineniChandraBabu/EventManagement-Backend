package com.familywishes.service;

import com.familywishes.dto.CircleDtos.*;

public interface CircleService {
  CircleListResponse listCircles();

  CircleRequestResponse sendRequest(SendCircleRequest request);

  CircleRequestResponse accept(CircleRequestAction request);

  void reject(CircleRequestAction request);

  void remove(CircleRequestAction request);

  CircleStatusResponse statusWith(Long userId);

  boolean areInCircle(Long userA, Long userB);

  /**
   * Emails a reminder to the addressee of every request that has been pending for at least {@code
   * days} and has not been reminded today. Returns how many reminders were sent.
   */
  int sendPendingReminders(int days);
}
