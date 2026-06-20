package com.example.dingtalk.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 统一返回给前端的用户状态: 手动设置状态 + WebSocket是否在线 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusVO {

    private Long userId;
    /** 手动设置状态: 1在线 2忙碌 3离开 4离线 */
    private Integer chatStatus;
    /** 是否有活跃 WebSocket 连接 */
    private Boolean online;
    /** 最后在线时间（用于展示“最后在线于 xx 分钟前”，可为空） */
    private LocalDateTime lastActiveAt;
}
