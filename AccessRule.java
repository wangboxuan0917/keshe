package com.keshe.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("access_rule")
public class AccessRule {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private Long userId;

    private Long roleId;

    /** 时间类型 1工作日 2周末 3节假日 4全天 5临时 */
    private Integer timeType;

    private LocalTime startTime;

    private LocalTime endTime;

    /** 允许星期(1-7,逗号分隔) */
    private String weekDays;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer priority;

    private Integer status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
