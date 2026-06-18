package com.example.dingtalk.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dingtalk.common.Result;
import com.example.dingtalk.common.SecurityUtils;
import com.example.dingtalk.entity.SysSchedule;
import com.example.dingtalk.mapper.ScheduleMapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

    private final ScheduleMapper scheduleMapper;

    public ScheduleController(ScheduleMapper scheduleMapper) {
        this.scheduleMapper = scheduleMapper;
    }

    @GetMapping("/list")
    public Result<List<SysSchedule>> list(@RequestParam(required = false) String start,
                                          @RequestParam(required = false) String end) {
        Long uid = SecurityUtils.getUserId();
        LambdaQueryWrapper<SysSchedule> w = new LambdaQueryWrapper<SysSchedule>()
                .eq(SysSchedule::getUserId, uid)
                .orderByAsc(SysSchedule::getStartTime);
        if (start != null) w.ge(SysSchedule::getStartTime, LocalDateTime.parse(start));
        if (end != null) w.le(SysSchedule::getStartTime, LocalDateTime.parse(end));
        return Result.ok(scheduleMapper.selectList(w));
    }

    @PostMapping
    public Result<?> add(@RequestBody SysSchedule schedule) {
        schedule.setUserId(SecurityUtils.getUserId());
        schedule.setCreateTime(LocalDateTime.now());
        scheduleMapper.insert(schedule);
        return Result.ok();
    }

    @PutMapping
    public Result<?> update(@RequestBody SysSchedule schedule) {
        scheduleMapper.updateById(schedule);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        scheduleMapper.deleteById(id);
        return Result.ok();
    }
}
