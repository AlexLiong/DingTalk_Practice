package com.example.dingtalk.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_notice_read")
public class SysNoticeRead {
    private Long id;
    private Long noticeId;
    private Long userId;
    private LocalDateTime readTime;
}
