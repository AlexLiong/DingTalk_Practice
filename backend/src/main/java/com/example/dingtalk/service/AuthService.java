package com.example.dingtalk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dingtalk.common.BizException;
import com.example.dingtalk.dto.LoginDTO;
import com.example.dingtalk.entity.SysUser;
import com.example.dingtalk.mapper.UserMapper;
import com.example.dingtalk.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final PermissionService permissionService;
    private final OnlineService onlineService;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil, PermissionService permissionService,
                       OnlineService onlineService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.permissionService = permissionService;
        this.onlineService = onlineService;
    }

    public Map<String, Object> login(LoginDTO dto) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername()));
        if (user == null) throw new BizException("用户不存在");
        if (user.getStatus() != null && user.getStatus() == 0) throw new BizException("账号已停用");
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BizException("密码错误");
        }
        if (user.getChatStatus() == null) user.setChatStatus(1);
        onlineService.cacheChatStatus(user.getId(), user.getChatStatus());
        onlineService.cacheLastActive(user.getId(), LocalDateTime.now());
        String token = jwtUtil.generate(user.getId(), user.getUsername());
        user.setPassword(null);
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", user);
        result.put("permissions", permissionService.getPerms(user.getId()));
        result.put("menus", permissionService.getMenuTree(user.getId()));
        return result;
    }

    public SysUser register(LoginDTO dto) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername()));
        if (count > 0) throw new BizException("用户名已存在");
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getUsername());
        user.setStatus(1);
        user.setChatStatus(1);
        userMapper.insert(user);
        onlineService.cacheChatStatus(user.getId(), 1);
        user.setPassword(null);
        return user;
    }

    public Map<String, Object> getInfo(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new BizException("用户不存在");
        if (user.getChatStatus() == null) user.setChatStatus(1);
        onlineService.cacheChatStatus(user.getId(), user.getChatStatus());
        onlineService.cacheLastActive(user.getId(), LocalDateTime.now());
        user.setOnline(onlineService.isOnline(userId));
        user.setPassword(null);
        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("permissions", permissionService.getPerms(userId));
        result.put("menus", permissionService.getMenuTree(userId));
        return result;
    }
}
