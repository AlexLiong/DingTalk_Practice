package com.example.dingtalk.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user_preference")
public class SysUserPreference {
    private Long id;
    private Long userId;
    private String theme;
    private String lastRoute;
    private String viewStateJson;
    private Integer connectedOnline;
    private LocalDateTime lastOnlineAt;
    private LocalDateTime lastOfflineAt;
    private LocalDateTime lastActiveAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
