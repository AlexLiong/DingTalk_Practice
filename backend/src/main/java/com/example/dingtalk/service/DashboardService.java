package com.example.dingtalk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dingtalk.entity.*;
import com.example.dingtalk.mapper.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** 数据看板 */
@Service
public class DashboardService {

    private final UserMapper userMapper;
    private final DeptMapper deptMapper;
    private final ChatMessageMapper messageMapper;
    private final ChatSessionMapper sessionMapper;
    private final TodoMapper todoMapper;
    private final ApprovalMapper approvalMapper;

    public DashboardService(UserMapper userMapper, DeptMapper deptMapper,
                            ChatMessageMapper messageMapper, ChatSessionMapper sessionMapper,
                            TodoMapper todoMapper, ApprovalMapper approvalMapper) {
        this.userMapper = userMapper;
        this.deptMapper = deptMapper;
        this.messageMapper = messageMapper;
        this.sessionMapper = sessionMapper;
        this.todoMapper = todoMapper;
        this.approvalMapper = approvalMapper;
    }

    public Map<String, Object> overview() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userCount", userMapper.selectCount(null));
        data.put("messageCount", messageMapper.selectCount(null));
        data.put("sessionCount", sessionMapper.selectCount(null));
        data.put("todoCount", todoMapper.selectCount(null));
        data.put("approvalCount", approvalMapper.selectCount(null));
        return data;
    }

    /** 部门人数分布 */
    public List<Map<String, Object>> deptDistribution() {
        List<SysDept> depts = deptMapper.selectList(
                new LambdaQueryWrapper<SysDept>().ne(SysDept::getParentId, 0));
        List<Map<String, Object>> list = new ArrayList<>();
        for (SysDept d : depts) {
            long count = userMapper.selectCount(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeptId, d.getId()));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", d.getName());
            m.put("value", count);
            list.add(m);
        }
        return list;
    }

    /** 近 7 日消息趋势 */
    public Map<String, Object> messageTrend() {
        List<String> dates = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            long count = messageMapper.selectCount(
                    new LambdaQueryWrapper<ChatMessage>()
                            .ge(ChatMessage::getCreateTime, start)
                            .lt(ChatMessage::getCreateTime, end));
            dates.add((date.getMonthValue()) + "/" + date.getDayOfMonth());
            counts.add(count);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dates", dates);
        result.put("counts", counts);
        return result;
    }
}
