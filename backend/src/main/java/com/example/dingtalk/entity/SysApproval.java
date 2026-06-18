package com.example.dingtalk.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_approval")
public class SysApproval {
    private Long id;
    private String type;           // leave请假 expense报销
    private String title;
    private String content;        // JSON: 事由/天数/金额
    private Long applicantId;
    private Long approverId;
    private Integer status;        // 0待审批 1通过 2驳回
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime approveTime;

    @TableField(exist = false)
    private String applicantName;
    @TableField(exist = false)
    private String approverName;
}
