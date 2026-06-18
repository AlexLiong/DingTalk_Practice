package com.example.dingtalk.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("chat_session")
public class ChatSession {
    private Long id;
    private Integer type;      // 1单聊 2群聊
    private String name;
    private String avatar;
    private Long ownerId;
    private String notice;     // 群公告
    private String lastMsg;
    private LocalDateTime lastTime;
    private LocalDateTime createTime;
}
