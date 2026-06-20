package com.example.dingtalk.controller;

import com.example.dingtalk.common.Result;
import com.example.dingtalk.service.OnlineService;
import com.example.dingtalk.vo.UserStatusVO;
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

    /** 获取当前所有在线用户的详细状态（在线 + 手动状态） */
    @GetMapping("/users")
    public Result<Set<UserStatusVO>> onlineUsers() {
        Set<Long> ids = onlineService.getOnlineUserIds();
        Set<UserStatusVO> result = new java.util.HashSet<>();
        for (Long uid : ids) {
            result.add(onlineService.getUserStatus(uid));
        }
        return Result.ok(result);
    }
}
