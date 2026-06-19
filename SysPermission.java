package com.keshe.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("sys_permission")
public class SysPermission {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String permission;

    /** 类型 1菜单 2按钮 3接口 */
    private Integer type;

    private Long parentId;

    private String path;

    private String icon;

    private Integer sort;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 子节点（非数据库字段） */
    @TableField(exist = false)
    private List<SysPermission> children;
}
