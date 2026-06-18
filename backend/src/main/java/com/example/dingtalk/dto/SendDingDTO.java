package com.example.dingtalk.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SendDingDTO {
    private Long targetUserId;
    private String title;
    private String content;
    private String type;
    private String scene;
    private String priority;
    private LocalDateTime deadlineTime;
}
