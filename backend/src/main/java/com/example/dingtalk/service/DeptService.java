package com.example.dingtalk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dingtalk.common.BizException;
import com.example.dingtalk.entity.SysDept;
import com.example.dingtalk.entity.SysUser;
import com.example.dingtalk.mapper.DeptMapper;
import com.example.dingtalk.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class DeptService {

    private final DeptMapper deptMapper;
    private final UserMapper userMapper;

    public DeptService(DeptMapper deptMapper, UserMapper userMapper) {
        this.deptMapper = deptMapper;
        this.userMapper = userMapper;
    }

    /** 部门树 */
    public List<SysDept> tree() {
        List<SysDept> all = deptMapper.selectList(
                new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getSort));
        Map<Long, SysDept> map = new HashMap<>();
        for (SysDept d : all) { d.setChildren(new ArrayList<>()); map.put(d.getId(), d); }
        List<SysDept> roots = new ArrayList<>();
        for (SysDept d : all) {
            if (d.getParentId() == null || d.getParentId() == 0 || !map.containsKey(d.getParentId())) {
                roots.add(d);
            } else {
                map.get(d.getParentId()).getChildren().add(d);
            }
        }
        return roots;
    }

    public void save(SysDept dept) {
        Long pid = dept.getParentId() == null ? 0L : dept.getParentId();
        dept.setParentId(pid);
        // 计算 ancestors
        if (pid == 0) {
            dept.setAncestors("0");
        } else {
            SysDept parent = deptMapper.selectById(pid);
            if (parent == null) throw new BizException("父部门不存在");
            dept.setAncestors(parent.getAncestors() + "," + pid);
        }
        if (dept.getStatus() == null) dept.setStatus(1);
        if (dept.getSort() == null) dept.setSort(0);
        dept.setCreateTime(LocalDateTime.now());
        deptMapper.insert(dept);
    }

    public void update(SysDept dept) {
        if (dept.getId() == null) throw new BizException("部门id不能为空");
        deptMapper.updateById(dept);
    }

    public void delete(Long id) {
        Long childCount = deptMapper.selectCount(
                new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id));
        if (childCount > 0) throw new BizException("存在子部门, 不能删除");
        Long userCount = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeptId, id));
        if (userCount > 0) throw new BizException("部门下存在员工, 不能删除");
        deptMapper.deleteById(id);
    }
}
