package com.example.dingtalk.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_todo")
public class SysTodo {
    private Long id;
    private String title;
    private String content;
    private Long creatorId;
    private Long assigneeId;
    private Integer priority;      // 1高 2中 3低
    private Integer status;        // 0待办 1进行中 2完成
    private LocalDateTime dueTime;
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String creatorName;
    @TableField(exist = false)
    private String assigneeName;
}
