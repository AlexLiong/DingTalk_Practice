package com.example.dingtalk.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_add_usage")
public class ChatAddUsage {
    private Long id;
    private Long userId;
    private String itemKey;
    private LocalDateTime useTime;
}
