package com.example.dingtalk.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WorkNoticeVO {
    private String category;
    private String action;
    private String level;
    private String title;
    private String summary;
    private String route;
    private Long refId;
    private Long mailboxUnread;
    private Long dingPending;
    private Long dingUrgent;
    private LocalDateTime createTime;
}
