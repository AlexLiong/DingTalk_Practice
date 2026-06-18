package com.example.dingtalk.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dingtalk.common.BizException;
import com.example.dingtalk.common.Result;
import com.example.dingtalk.entity.SysMenu;
import com.example.dingtalk.mapper.MenuMapper;
import com.example.dingtalk.service.PermissionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
public class MenuController {

    private final MenuMapper menuMapper;
    private final PermissionService permissionService;

    public MenuController(MenuMapper menuMapper, PermissionService permissionService) {
        this.menuMapper = menuMapper;
        this.permissionService = permissionService;
    }

    /** 全部菜单树 (菜单管理 / 角色分配权限用) */
    @GetMapping("/tree")
    public Result<List<SysMenu>> tree() {
        return Result.ok(permissionService.getAllMenuTree());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:menu:add')")
    public Result<?> add(@RequestBody SysMenu menu) {
        if (menu.getParentId() == null) menu.setParentId(0L);
        if (menu.getSort() == null) menu.setSort(0);
        if (menu.getVisible() == null) menu.setVisible(1);
        menuMapper.insert(menu);
        return Result.ok();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('system:menu:edit')")
    public Result<?> update(@RequestBody SysMenu menu) {
        menuMapper.updateById(menu);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:remove')")
    public Result<?> delete(@PathVariable Long id) {
        Long childCount = menuMapper.selectCount(
                new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id));
        if (childCount > 0) throw new BizException("存在子菜单, 不能删除");
        menuMapper.deleteById(id);
        return Result.ok();
    }
}
