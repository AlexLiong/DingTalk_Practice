package com.example.dingtalk.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("chat_message_reaction")
public class ChatMessageReaction {
    private Long id;
    private Long messageId;
    private Long userId;
    private String emoji;
    private LocalDateTime createTime;
}
