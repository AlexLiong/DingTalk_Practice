package com.example.dingtalk.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_saved_link")
public class ChatSavedLink {
    private Long id;
    private Long userId;
    private String title;
    private String url;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
