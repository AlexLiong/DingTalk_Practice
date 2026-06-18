package com.example.dingtalk.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dingtalk.common.Result;
import com.example.dingtalk.common.SecurityUtils;
import com.example.dingtalk.entity.SysUser;
import com.example.dingtalk.mapper.UserMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 通讯录 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserMapper userMapper;

    public UserController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /** 通讯录: 所有用户(不含自己, 去密码) */
    @GetMapping
    public Result<List<SysUser>> list() {
        Long me = SecurityUtils.getUserId();
        List<SysUser> users = userMapper.selectList(
                new LambdaQueryWrapper<SysUser>().ne(SysUser::getId, me));
        users.forEach(u -> u.setPassword(null));
        return Result.ok(users);
    }
}
