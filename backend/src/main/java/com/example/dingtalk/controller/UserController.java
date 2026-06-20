package com.example.dingtalk.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dingtalk.common.Result;
import com.example.dingtalk.common.SecurityUtils;
import com.example.dingtalk.entity.SysUser;
import com.example.dingtalk.mapper.UserMapper;
import com.example.dingtalk.service.OnlineService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 通讯录 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserMapper userMapper;
    private final OnlineService onlineService;

    public UserController(UserMapper userMapper, OnlineService onlineService) {
        this.userMapper = userMapper;
        this.onlineService = onlineService;
    }

    /** 通讯录: 所有用户(不含自己, 去密码, 带在线状态) */
    @GetMapping
    public Result<List<SysUser>> list() {
        Long me = SecurityUtils.getUserId();
        List<SysUser> users = userMapper.selectList(
                new LambdaQueryWrapper<SysUser>().ne(SysUser::getId, me));
        users.forEach(u -> {
            u.setPassword(null);
            // 给每个用户标注：是否活跃在线 + 手动状态默认补齐
            if (u.getChatStatus() == null) u.setChatStatus(1);
            u.setOnline(onlineService.isOnline(u.getId()));
            // 同时缓存在线状态服务，避免其他接口重复查库
            onlineService.cacheChatStatus(u.getId(), u.getChatStatus());
        });
        return Result.ok(users);
    }
}
