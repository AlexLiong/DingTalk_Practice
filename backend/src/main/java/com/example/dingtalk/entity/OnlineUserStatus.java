package com.example.dingtalk.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OnlineUserStatus {
    // 用户手动设置状态：1在线 2忙碌 3离开
    private Integer chatStatus;
    // 最后心跳时间（用来判断超时离线）
    private LocalDateTime lastHeartbeat;
    // WS是否在线连接
    private boolean online;
}
