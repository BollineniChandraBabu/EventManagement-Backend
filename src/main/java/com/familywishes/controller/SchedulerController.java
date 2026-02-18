package com.familywishes.controller;

import com.familywishes.dto.SchedulerDtos.SchedulerStatusResponse;
import com.familywishes.dto.SchedulerDtos.SchedulerTriggerResponse;
import com.familywishes.service.SchedulerManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/schedulers")
@RequiredArgsConstructor
public class SchedulerController {

    private final SchedulerManagementService schedulerManagementService;

    @GetMapping
    public ResponseEntity<List<SchedulerStatusResponse>> getAllSchedulers() {
        return ResponseEntity.ok(schedulerManagementService.getAllStatuses());
    }

    @PostMapping("/{jobName}/trigger")
    public ResponseEntity<SchedulerTriggerResponse> triggerScheduler(@PathVariable String jobName) {
        return ResponseEntity.ok(schedulerManagementService.trigger(jobName));
    }
}
