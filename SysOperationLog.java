package com.keshe.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_operation_log")
public class SysOperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String userName;

    private String module;

    private String operation;

    private String requestUrl;

    private String method;

    private String ip;

    private String params;

    /** 结果 1成功 0失败 */
    private Integer result;

    private Long costTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
