package com.example.dingtalk.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_notice")
public class SysNotice {
    private Long id;
    private String title;
    private String content;
    private Integer type;          // 1通知 2公告
    private Integer priority;      // 1紧急 2普通
    private Long publisherId;
    private Integer status;        // 1已发布 0草稿
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String publisherName;
    @TableField(exist = false)
    private Integer readCount;
    @TableField(exist = false)
    private Integer totalCount;
    @TableField(exist = false)
    private Boolean hasRead;
}
