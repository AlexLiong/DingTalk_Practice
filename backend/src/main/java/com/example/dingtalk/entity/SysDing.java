package com.example.dingtalk.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("sys_ding")
public class SysDing {
    private Long id;
    private Long userId;
    private Long ownerUserId;
    private Long targetUserId;
    private String linkKey;
    private String title;
    private String content;
    private String direction;
    private String ownerName;
    private String targetName;
    private String type;
    private String scene;
    private String status;
    private String priority;
    private LocalDateTime deadlineTime;
    private LocalDateTime sentTime;
    private LocalDateTime updateTime;
    private Integer remindCount;
    private String note;
    private String useCasesJson;
    private String historyJson;
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String owner;
    @TableField(exist = false)
    private String target;
    @TableField(exist = false)
    private String deadline;
    @TableField(exist = false)
    private String sentAt;
    @TableField(exist = false)
    private String updatedAt;
    @TableField(exist = false)
    private List<UseCaseItem> useCases;
    @TableField(exist = false)
    private List<HistoryItem> history;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UseCaseItem {
        private String title;
        private String description;
        private String tag;
        private String action;
        private String actionLabel;
        private String template;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryItem {
        private String time;
        private String text;
    }
}
