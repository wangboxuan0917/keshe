package com.keshe.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String realName;

    private Integer gender;

    private String phone;

    private String email;

    private String college;

    private String className;

    private String avatar;

    private Integer status;

    private Integer type;

    private String remark;

    private String cardNo;

    @TableField(exist = false)
    private List<Long> roleIds;

    @TableField(exist = false)
    private String plainPassword;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
