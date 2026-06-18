package com.example.dingtalk.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 消息项 (含发送者信息) */
@Data
public class MessageVO {
    private Long id;
    private Long sessionId;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private Integer contentType;
    private String content;
    private String extra;
    private String atUserIds;
    private Integer status;        // 1正常 0撤回
    private LocalDateTime createTime;
    private Integer readCount;     // 已读人数 (从 chat_message_read 查)
    private List<Map<String, Object>> reactions;  // 表情反应列表
}
