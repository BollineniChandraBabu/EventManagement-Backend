package com.familywishes.controller;

import com.familywishes.dto.DashboardDtos.DashboardResponse;
import com.familywishes.entity.MessageLog;
import com.familywishes.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping(value = "/mail")
    public DashboardResponse getMailDashboard() {
        return dashboardService.getDashboard();
    }

    @GetMapping(value = "/insta")
    public DashboardResponse getIGDashboard() {
        return dashboardService.getIGDashboard();
    }

    @GetMapping("/chart")
    public List<Map<String, Object>> chart(
            @RequestParam(defaultValue = "7") int days) {
        return dashboardService.getChart(days);

    }

}
