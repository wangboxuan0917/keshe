package com.keshe.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_notice")
public class SysNotice {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    /** 类型 1安全通知 2运维通知 3管理通知 4系统通知 */
    private Integer type;

    /** 状态 0草稿 1已发布 2已下架 */
    private Integer status;

    private LocalDateTime publishTime;

    private String publisher;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
