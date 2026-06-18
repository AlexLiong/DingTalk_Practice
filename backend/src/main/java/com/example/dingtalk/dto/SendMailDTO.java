package com.example.dingtalk.dto;

import lombok.Data;

import java.util.List;

@Data
public class SendMailDTO {
    private List<Long> recipientIds;
    private List<Long> ccIds;
    private List<Long> attachmentIds;
    private String subject;
    private String content;
    private String priority;
    private String tag;
}
