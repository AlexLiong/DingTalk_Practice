package com.example.dingtalk.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("sys_role")
public class SysRole {
    private Long id;
    private String name;
    private String roleKey;
    private Integer sort;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;

    @TableField(exist = false)
    private List<Long> menuIds;   // 该角色拥有的菜单id
}
