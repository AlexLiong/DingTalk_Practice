package com.example.dingtalk.controller;

import com.example.dingtalk.common.Result;
import com.example.dingtalk.service.OnlineService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/online")
public class OnlineController {

    private final OnlineService onlineService;

    public OnlineController(OnlineService onlineService) {
        this.onlineService = onlineService;
    }

    @GetMapping("/users")
    public Result<Set<Long>> onlineUsers() {
        return Result.ok(onlineService.getOnlineUserIds());
    }
}
