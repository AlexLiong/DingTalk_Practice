package com.example.dingtalk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dingtalk.common.BizException;
import com.example.dingtalk.entity.SysDept;
import com.example.dingtalk.entity.SysUser;
import com.example.dingtalk.mapper.DeptMapper;
import com.example.dingtalk.mapper.UserMapper;
import com.example.dingtalk.mapper.UserRoleMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserAdminService {

    private final UserMapper userMapper;
    private final DeptMapper deptMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public UserAdminService(UserMapper userMapper, DeptMapper deptMapper,
                            UserRoleMapper userRoleMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.deptMapper = deptMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public IPage<SysUser> page(int pageNum, int pageSize, String keyword, Long deptId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .like(keyword != null && !keyword.isBlank(), SysUser::getNickname, keyword)
                .eq(deptId != null, SysUser::getDeptId, deptId)
                .orderByDesc(SysUser::getId);
        IPage<SysUser> page = userMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        page.getRecords().forEach(u -> u.setPassword(null));
        return page;
    }

    @Transactional
    public void save(SysUser user, List<Long> roleIds) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, user.getUsername()));
        if (count > 0) throw new BizException("用户名已存在");
        user.setPassword(passwordEncoder.encode(
                user.getPassword() == null || user.getPassword().isBlank() ? "Boz@2026" : user.getPassword()));
        fillDeptName(user);
        if (user.getStatus() == null) user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        saveRoles(user.getId(), roleIds);
    }

    @Transactional
    public void update(SysUser user, List<Long> roleIds) {
        if (user.getId() == null) throw new BizException("用户id不能为空");
        user.setPassword(null);   // 不在此处改密码
        user.setUsername(null);   // 不允许改账号
        fillDeptName(user);
        userMapper.updateById(user);
        if (roleIds != null) {
            userRoleMapper.deleteByUserId(user.getId());
            saveRoles(user.getId(), roleIds);
        }
    }

    @Transactional
    public void delete(Long id) {
        userRoleMapper.deleteByUserId(id);
        userMapper.deleteById(id);
    }

    public void resetPassword(Long id, String password) {
        SysUser u = new SysUser();
        u.setId(id);
        u.setPassword(passwordEncoder.encode(
                password == null || password.isBlank() ? "Boz@2026" : password));
        userMapper.updateById(u);
    }

    public List<Long> getRoleIds(Long userId) {
        return userRoleMapper.selectRoleIdsByUserId(userId);
    }

    private void fillDeptName(SysUser user) {
        if (user.getDeptId() != null) {
            SysDept dept = deptMapper.selectById(user.getDeptId());
            if (dept != null) user.setDeptName(dept.getName());
        }
    }

    private void saveRoles(Long userId, List<Long> roleIds) {
        if (roleIds == null) return;
        for (Long roleId : roleIds) userRoleMapper.insert(userId, roleId);
    }
}
