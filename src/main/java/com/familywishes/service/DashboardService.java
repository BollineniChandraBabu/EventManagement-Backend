package com.familywishes.service;

import com.familywishes.dto.DashboardDtos.DashboardResponse;

import java.util.List;
import java.util.Map;

public interface DashboardService {
    DashboardResponse getDashboard();
    DashboardResponse getIGDashboard();
    List<Map<String, Object>> getChart(int days);
}
