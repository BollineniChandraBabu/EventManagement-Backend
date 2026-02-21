package com.familywishes.controller;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.SchedulerDtos.SchedulerStatusResponse;
import com.familywishes.dto.SchedulerDtos.SchedulerTriggerResponse;
import com.familywishes.service.SchedulerManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schedulers")
@RequiredArgsConstructor
public class SchedulerController {

  private final SchedulerManagementService schedulerManagementService;

  @GetMapping
  public ResponseEntity<PagedResponse<SchedulerStatusResponse>> getAllSchedulers(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "") String searchKey) {
    return ResponseEntity.ok(schedulerManagementService.getAllStatuses(page, size, searchKey));
  }

  @PostMapping("/{jobName}/trigger")
  public ResponseEntity<SchedulerTriggerResponse> triggerScheduler(@PathVariable String jobName) {
    return ResponseEntity.ok(schedulerManagementService.trigger(jobName));
  }
}
