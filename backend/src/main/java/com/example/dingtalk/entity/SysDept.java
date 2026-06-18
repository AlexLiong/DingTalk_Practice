package com.example.dingtalk.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@TableName("sys_dept")
public class SysDept {
    private Long id;
    private Long parentId;
    private String name;
    private String ancestors;
    private Integer sort;
    private String leader;
    private Integer status;
    private LocalDateTime createTime;

    @TableField(exist = false)
    private List<SysDept> children = new ArrayList<>();
}
