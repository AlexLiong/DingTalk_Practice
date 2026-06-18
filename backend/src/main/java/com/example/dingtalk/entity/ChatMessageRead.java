package com.example.dingtalk.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("chat_message_read")
public class ChatMessageRead {
    private Long id;
    private Long messageId;
    private Long sessionId;
    private Long userId;
    private LocalDateTime readTime;
}
