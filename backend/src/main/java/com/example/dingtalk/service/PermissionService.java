package com.example.dingtalk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dingtalk.entity.SysMenu;
import com.example.dingtalk.mapper.MenuMapper;
import com.example.dingtalk.mapper.RoleMenuMapper;
import com.example.dingtalk.mapper.UserRoleMapper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/** 权限/菜单查询 */
@Service
public class PermissionService {

    private final UserRoleMapper userRoleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final MenuMapper menuMapper;

    public PermissionService(UserRoleMapper userRoleMapper, RoleMenuMapper roleMenuMapper,
                             MenuMapper menuMapper) {
        this.userRoleMapper = userRoleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.menuMapper = menuMapper;
    }

    /** 用户拥有的菜单id集合 */
    public Set<Long> getMenuIds(Long userId) {
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(userId);
        Set<Long> menuIds = new HashSet<>();
        for (Long roleId : roleIds) {
            menuIds.addAll(roleMenuMapper.selectMenuIdsByRoleId(roleId));
        }
        return menuIds;
    }

    /** 用户的权限标识集合 (用于 @PreAuthorize) */
    public Set<String> getPerms(Long userId) {
        Set<Long> menuIds = getMenuIds(userId);
        if (menuIds.isEmpty()) return Collections.emptySet();
        List<SysMenu> menus = menuMapper.selectBatchIds(menuIds);
        return menus.stream()
                .map(SysMenu::getPerms)
                .filter(p -> p != null && !p.isBlank())
                .collect(Collectors.toSet());
    }

    /** 用户的角色key集合 */
    public List<Long> getRoleIds(Long userId) {
        return userRoleMapper.selectRoleIdsByUserId(userId);
    }

    /** 用户可见的菜单树 (type 1目录 2菜单) */
    public List<SysMenu> getMenuTree(Long userId) {
        Set<Long> menuIds = getMenuIds(userId);
        List<SysMenu> menus;
        if (menuIds.isEmpty()) {
            menus = new ArrayList<>();
        } else {
            menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                    .in(SysMenu::getId, menuIds)
                    .in(SysMenu::getType, 1, 2)
                    .orderByAsc(SysMenu::getSort));
        }
        return buildTree(menus);
    }

    /** 全部菜单树 (后台菜单管理用) */
    public List<SysMenu> getAllMenuTree() {
        List<SysMenu> menus = menuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSort));
        return buildTree(menus);
    }

    private List<SysMenu> buildTree(List<SysMenu> menus) {
        Map<Long, SysMenu> map = new HashMap<>();
        for (SysMenu m : menus) { m.setChildren(new ArrayList<>()); map.put(m.getId(), m); }
        List<SysMenu> roots = new ArrayList<>();
        for (SysMenu m : menus) {
            if (m.getParentId() == null || m.getParentId() == 0 || !map.containsKey(m.getParentId())) {
                roots.add(m);
            } else {
                map.get(m.getParentId()).getChildren().add(m);
            }
        }
        return roots;
    }
}
