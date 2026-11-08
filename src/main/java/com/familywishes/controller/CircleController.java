package com.familywishes.controller;

import com.familywishes.dto.CircleDtos.*;
import com.familywishes.service.CircleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/circles")
@RequiredArgsConstructor
public class CircleController {
  private final CircleService circleService;

  @GetMapping
  public CircleListResponse list() {
    return circleService.listCircles();
  }

  @PostMapping("/requests")
  public CircleRequestResponse send(@Valid @RequestBody SendCircleRequest request) {
    return circleService.sendRequest(request);
  }

  @PostMapping("/requests/accept")
  public CircleRequestResponse accept(@Valid @RequestBody CircleRequestAction request) {
    return circleService.accept(request);
  }

  @PostMapping("/requests/reject")
  public void reject(@Valid @RequestBody CircleRequestAction request) {
    circleService.reject(request);
  }

  @DeleteMapping
  public void remove(@Valid @RequestBody CircleRequestAction request) {
    circleService.remove(request);
  }

  @GetMapping("/status/{userId}")
  public CircleStatusResponse status(@PathVariable Long userId) {
    return circleService.statusWith(userId);
  }
}
