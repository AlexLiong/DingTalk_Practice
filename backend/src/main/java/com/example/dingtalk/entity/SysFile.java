package com.example.dingtalk.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("sys_file")
public class SysFile {
    private Long id;
    private String name;
    private String url;
    private Long size;
    private String type;
    private Long uploaderId;
    private String scene;
    private String description;
    private String status;
    private Integer shareCount;
    private String historyJson;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private List<HistoryItem> history;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryItem {
        private String time;
        private String text;
    }
}
