package com.example.dingtalk.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dingtalk.common.BizException;
import com.example.dingtalk.entity.SysRole;
import com.example.dingtalk.mapper.RoleMapper;
import com.example.dingtalk.mapper.RoleMenuMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RoleService {

    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;

    public RoleService(RoleMapper roleMapper, RoleMenuMapper roleMenuMapper) {
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
    }

    public IPage<SysRole> page(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .like(keyword != null && !keyword.isBlank(), SysRole::getName, keyword)
                .orderByAsc(SysRole::getSort);
        return roleMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    public List<SysRole> all() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getSort));
    }

    @Transactional
    public void save(SysRole role) {
        if (role.getStatus() == null) role.setStatus(1);
        if (role.getSort() == null) role.setSort(0);
        role.setCreateTime(LocalDateTime.now());
        roleMapper.insert(role);
        saveMenus(role.getId(), role.getMenuIds());
    }

    @Transactional
    public void update(SysRole role) {
        if (role.getId() == null) throw new BizException("角色id不能为空");
        // 判断是否有需要更新的主表字段
        boolean hasUpdateField = role.getName() != null
                || role.getRoleKey() != null
                || role.getSort() != null
                || role.getStatus() != null
                || role.getRemark() != null
                || role.getCreateTime() != null;

        // 只有存在要更新的字段才执行updateById，避免空SET报错
        if (hasUpdateField) {
            roleMapper.updateById(role);
        }
        if (role.getMenuIds() != null) {
            roleMenuMapper.deleteByRoleId(role.getId());
            saveMenus(role.getId(), role.getMenuIds());
        }
    }

    @Transactional
    public void delete(Long id) {
        roleMenuMapper.deleteByRoleId(id);
        roleMapper.deleteById(id);
    }

    /** 角色已分配的菜单id */
    public List<Long> getMenuIds(Long roleId) {
        return roleMenuMapper.selectMenuIdsByRoleId(roleId);
    }

    private void saveMenus(Long roleId, List<Long> menuIds) {
        if (menuIds == null) return;
        for (Long menuId : menuIds) {
            roleMenuMapper.insert(roleId, menuId);
        }
    }
}
