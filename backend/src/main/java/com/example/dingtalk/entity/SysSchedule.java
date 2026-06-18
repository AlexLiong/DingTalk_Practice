package com.example.dingtalk.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_schedule")
public class SysSchedule {
    private Long id;
    private String title;
    private String content;
    private Long userId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer allDay;
    private String color;
    private LocalDateTime createTime;
}
