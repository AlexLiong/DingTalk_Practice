package com.example.dingtalk.controller;

import com.example.dingtalk.common.BizException;
import com.example.dingtalk.common.Result;
import com.example.dingtalk.common.SecurityUtils;
import com.example.dingtalk.entity.SysUser;
import com.example.dingtalk.mapper.UserMapper;
import com.example.dingtalk.service.OnlineService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 个人中心 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final OnlineService onlineService;

    public ProfileController(UserMapper userMapper, PasswordEncoder passwordEncoder,
                             OnlineService onlineService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.onlineService = onlineService;
    }

    /** 修改个人资料 (昵称/手机/邮箱/性别/头像/聊天状态) */
    @PutMapping
    public Result<?> update(@RequestBody SysUser form) {
        Long uid = SecurityUtils.getUserId();
        if (form.getChatStatus() != null && (form.getChatStatus() < 1 || form.getChatStatus() > 4)) {
            throw new BizException("聊天状态不合法");
        }
        boolean statusChanged = form.getChatStatus() != null;
        SysUser u = new SysUser();
        u.setId(uid);
        u.setNickname(form.getNickname());
        u.setMobile(form.getMobile());
        u.setEmail(form.getEmail());
        u.setGender(form.getGender());
        u.setAvatar(form.getAvatar());
        u.setChatStatus(form.getChatStatus());
        userMapper.updateById(u);
        SysUser fresh = userMapper.selectById(uid);
        fresh.setPassword(null);
        fresh.setOnline(onlineService.isOnline(uid));
        if (statusChanged) {
            // 向所有在线用户广播：此人的状态发生了变化
            onlineService.broadcastManualStatusChange(uid, fresh.getChatStatus());
        }
        return Result.ok(fresh);
    }

    /** 修改密码 */
    @PutMapping("/password")
    public Result<?> changePassword(@RequestBody Map<String, String> body) {
        Long uid = SecurityUtils.getUserId();
        String oldPwd = body.get("oldPassword");
        String newPwd = body.get("newPassword");
        if (newPwd == null || newPwd.length() < 6) throw new BizException("新密码至少6位");
        SysUser u = userMapper.selectById(uid);
        if (!passwordEncoder.matches(oldPwd, u.getPassword())) {
            throw new BizException("原密码错误");
        }
        SysUser update = new SysUser();
        update.setId(uid);
        update.setPassword(passwordEncoder.encode(newPwd));
        userMapper.updateById(update);
        return Result.ok();
    }
}
