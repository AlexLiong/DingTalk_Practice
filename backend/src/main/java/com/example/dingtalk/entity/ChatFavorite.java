package com.example.dingtalk.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("chat_favorite")
public class ChatFavorite {
    private Long id;
    private Long userId;
    private Long messageId;
    private Long sessionId;
    private String content;
    private String senderName;
    private LocalDateTime createTime;
}
