package com.example.dingtalk.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.dingtalk.common.Result;
import com.example.dingtalk.entity.SysRole;
import com.example.dingtalk.service.RoleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/role")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping("/page")
    public Result<IPage<SysRole>> page(@RequestParam(defaultValue = "1") int pageNum,
                                       @RequestParam(defaultValue = "10") int pageSize,
                                       @RequestParam(required = false) String keyword) {
        return Result.ok(roleService.page(pageNum, pageSize, keyword));
    }

    @GetMapping("/all")
    public Result<List<SysRole>> all() {
        return Result.ok(roleService.all());
    }

    @GetMapping("/{id}/menus")
    public Result<List<Long>> menuIds(@PathVariable Long id) {
        return Result.ok(roleService.getMenuIds(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:role:add')")
    public Result<?> add(@RequestBody SysRole role) {
        roleService.save(role);
        return Result.ok();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('system:role:edit')")
    public Result<?> update(@RequestBody SysRole role) {
        roleService.update(role);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:remove')")
    public Result<?> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.ok();
    }
}
