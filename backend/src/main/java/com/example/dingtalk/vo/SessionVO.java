package com.example.dingtalk.vo;

import lombok.Data;
import java.time.LocalDateTime;

/** 会话列表项 (已组装展示信息) */
@Data
public class SessionVO {
    private Long id;
    private Integer type;       // 1单聊 2群聊
    private String name;        // 单聊=对方昵称, 群聊=群名
    private String avatar;      // 展示头像
    private String lastMsg;
    private LocalDateTime lastTime;
    private Integer unread;
    private Long targetUserId;  // 单聊时对方用户id
    private Integer isTop;      // 是否置顶
    private Integer isMute;
    private Integer isStar;
}
