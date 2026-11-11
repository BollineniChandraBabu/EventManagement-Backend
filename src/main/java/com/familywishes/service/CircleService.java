package com.familywishes.service;

import com.familywishes.dto.CircleDtos.*;
import java.util.List;

public interface CircleService {
  CircleListResponse listCircles();

  CircleRequestResponse sendRequest(SendCircleRequest request);

  CircleRequestResponse accept(CircleRequestAction request);

  void reject(CircleRequestAction request);

  void remove(CircleRequestAction request);

  CircleStatusResponse statusWith(Long userId);

  boolean areInCircle(Long userA, Long userB);

  /**
   * Ids of the users who are in an accepted circle with the given user (both directions). Used to
   * include a circle member's birthdays and events on the user's calendar.
   */
  List<Long> acceptedCircleMemberIds(Long userId);

  /**
   * Emails a reminder to the addressee of every request that has been pending for at least {@code
   * days} and has not been reminded today. Returns how many reminders were sent.
   */
  int sendPendingReminders(int days);
}
