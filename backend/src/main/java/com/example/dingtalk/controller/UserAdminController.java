package com.example.dingtalk.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.dingtalk.common.Result;
import com.example.dingtalk.entity.SysUser;
import com.example.dingtalk.service.UserAdminService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 后台用户管理 */
@RestController
@RequestMapping("/api/admin/user")
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping("/page")
    public Result<IPage<SysUser>> page(@RequestParam(defaultValue = "1") int pageNum,
                                       @RequestParam(defaultValue = "10") int pageSize,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) Long deptId) {
        return Result.ok(userAdminService.page(pageNum, pageSize, keyword, deptId));
    }

    @GetMapping("/{id}/roles")
    public Result<List<Long>> roleIds(@PathVariable Long id) {
        return Result.ok(userAdminService.getRoleIds(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:user:add')")
    public Result<?> add(@RequestBody SysUser user) {
        userAdminService.save(user, user.getRoleIds());
        return Result.ok();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('system:user:edit')")
    public Result<?> update(@RequestBody SysUser user) {
        userAdminService.update(user, user.getRoleIds());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:remove')")
    public Result<?> delete(@PathVariable Long id) {
        userAdminService.delete(id);
        return Result.ok();
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("hasAuthority('system:user:resetPwd')")
    public Result<?> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        userAdminService.resetPassword(id, body.get("password"));
        return Result.ok();
    }
}
