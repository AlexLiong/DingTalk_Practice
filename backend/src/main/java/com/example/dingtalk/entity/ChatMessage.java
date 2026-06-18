package com.example.dingtalk.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("chat_message")
public class ChatMessage {
    private Long id;
    private Long sessionId;
    private Long senderId;
    private Integer contentType;  // 1文本 2图片 3文件 4系统 5视频
    private String content;
    private String extra;         // 文件信息 json
    private String atUserIds;     // @的用户id, 逗号分隔
    private Integer status;       // 1正常 0撤回
    private LocalDateTime createTime;
}
