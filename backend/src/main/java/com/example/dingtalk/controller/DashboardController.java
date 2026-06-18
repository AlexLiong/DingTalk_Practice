package com.example.dingtalk.controller;

import com.example.dingtalk.common.Result;
import com.example.dingtalk.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(dashboardService.overview());
    }

    @GetMapping("/dept-distribution")
    public Result<List<Map<String, Object>>> deptDistribution() {
        return Result.ok(dashboardService.deptDistribution());
    }

    @GetMapping("/message-trend")
    public Result<Map<String, Object>> messageTrend() {
        return Result.ok(dashboardService.messageTrend());
    }
}
