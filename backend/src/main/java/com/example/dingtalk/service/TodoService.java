package com.example.dingtalk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dingtalk.common.BizException;
import com.example.dingtalk.entity.SysTodo;
import com.example.dingtalk.entity.SysUser;
import com.example.dingtalk.mapper.TodoMapper;
import com.example.dingtalk.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TodoService {

    private final TodoMapper todoMapper;
    private final UserMapper userMapper;

    public TodoService(TodoMapper todoMapper, UserMapper userMapper) {
        this.todoMapper = todoMapper;
        this.userMapper = userMapper;
    }

    /** 我的待办 (我创建的 + 分配给我的) */
    public List<SysTodo> myList(Long userId, Integer status) {
        LambdaQueryWrapper<SysTodo> w = new LambdaQueryWrapper<SysTodo>()
                .and(q -> q.eq(SysTodo::getCreatorId, userId)
                        .or().eq(SysTodo::getAssigneeId, userId))
                .eq(status != null, SysTodo::getStatus, status)
                .orderByAsc(SysTodo::getStatus)
                .orderByDesc(SysTodo::getCreateTime);
        List<SysTodo> list = todoMapper.selectList(w);
        list.forEach(this::fillNames);
        return list;
    }

    /** 统计: 各状态数量 */
    public long[] stats(Long userId) {
        long[] s = new long[3];
        for (int i = 0; i < 3; i++) {
            s[i] = todoMapper.selectCount(new LambdaQueryWrapper<SysTodo>()
                    .and(q -> q.eq(SysTodo::getCreatorId, userId).or().eq(SysTodo::getAssigneeId, userId))
                    .eq(SysTodo::getStatus, i));
        }
        return s;
    }

    public void save(SysTodo todo, Long creatorId) {
        todo.setCreatorId(creatorId);
        if (todo.getAssigneeId() == null) todo.setAssigneeId(creatorId);
        if (todo.getStatus() == null) todo.setStatus(0);
        if (todo.getPriority() == null) todo.setPriority(2);
        todo.setCreateTime(LocalDateTime.now());
        todoMapper.insert(todo);
    }

    public void update(SysTodo todo) {
        if (todo.getId() == null) throw new BizException("待办id不能为空");
        todoMapper.updateById(todo);
    }

    public void delete(Long id) {
        todoMapper.deleteById(id);
    }

    public void updateStatus(Long id, int status) {
        SysTodo t = new SysTodo();
        t.setId(id);
        t.setStatus(status);
        todoMapper.updateById(t);
    }

    private void fillNames(SysTodo t) {
        SysUser creator = userMapper.selectById(t.getCreatorId());
        if (creator != null) t.setCreatorName(creator.getNickname());
        SysUser assignee = userMapper.selectById(t.getAssigneeId());
        if (assignee != null) t.setAssigneeName(assignee.getNickname());
    }
}
