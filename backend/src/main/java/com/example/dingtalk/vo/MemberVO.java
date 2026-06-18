package com.example.dingtalk.vo;

import lombok.Data;

/** 群成员 */
@Data
public class MemberVO {
    private Long userId;
    private String nickname;
    private String avatar;
    private String jobTitle;
    private String deptName;
    private Integer memberRole;   // 1群主 2普通
}
