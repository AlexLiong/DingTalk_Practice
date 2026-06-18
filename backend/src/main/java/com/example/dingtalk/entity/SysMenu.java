package com.example.dingtalk.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
@TableName("sys_menu")
public class SysMenu {
    private Long id;
    private Long parentId;
    private String name;
    private Integer type;        // 1目录 2菜单 3按钮
    private String path;
    private String icon;
    private String perms;
    private Integer sort;
    private Integer visible;

    @TableField(exist = false)
    private List<SysMenu> children = new ArrayList<>();
}
