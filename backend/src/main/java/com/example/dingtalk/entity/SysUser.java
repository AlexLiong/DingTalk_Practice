package com.example.dingtalk.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("sys_user")
public class SysUser {
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String mobile;
    private String email;
    private Integer gender;       // 0未知 1男 2女
    private String jobTitle;
    private Long deptId;
    private String deptName;
    private Integer status;
    private Integer chatStatus;   // 1在线 2忙碌 3离开 4离线
    private String remark;
    private LocalDateTime createTime;

    @TableField(exist = false)
    private Boolean online;
    @TableField(exist = false)
    private List<Long> roleIds;
}
