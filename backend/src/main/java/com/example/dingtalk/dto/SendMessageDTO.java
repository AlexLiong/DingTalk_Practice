package com.example.dingtalk.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendMessageDTO {
    @NotNull(message = "会话id不能为空")
    private Long sessionId;
    private String content;
    private Integer contentType = 1;   // 1文本 2图片 3文件 4系统 5视频
    private String extra;              // 文件信息 json
    private String atUserIds;          // @的用户id, 逗号分隔
}
