package com.example.dingtalk.controller;

import com.example.dingtalk.common.Result;
import com.example.dingtalk.common.SecurityUtils;
import com.example.dingtalk.dto.UpdateUserPreferenceDTO;
import com.example.dingtalk.service.UserPreferenceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/user-preference")
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;

    public UserPreferenceController(UserPreferenceService userPreferenceService) {
        this.userPreferenceService = userPreferenceService;
    }

    @GetMapping
    public Result<Map<String, Object>> current() {
        return Result.ok(userPreferenceService.getCurrent(SecurityUtils.getUserId()));
    }

    @PutMapping
    public Result<Map<String, Object>> update(@RequestBody UpdateUserPreferenceDTO dto) {
        return Result.ok(userPreferenceService.updateCurrent(SecurityUtils.getUserId(), dto));
    }
}
