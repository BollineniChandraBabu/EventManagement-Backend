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
}
