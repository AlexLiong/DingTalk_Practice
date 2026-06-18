package com.example.dingtalk.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("chat_session_member")
public class ChatSessionMember {
    private Long id;
    private Long sessionId;
    private Long userId;
    private Integer memberRole;   // 1群主 2普通成员
    private Integer unread;
    private Integer isTop;
    private Integer isMute;       // 免打扰
    private Integer isStar;       // 特别关注
    private LocalDateTime joinTime;
}
