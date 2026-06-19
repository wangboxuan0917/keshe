package com.keshe.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("attendance_rule")
public class AttendanceRule {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private Long deviceId;

    /** 关联角色ID */
    private Long roleId;

    private LocalTime checkInStart;

    private LocalTime checkInEnd;

    private LocalTime checkInLateEnd;

    private LocalTime checkOutStart;

    private LocalTime checkOutEnd;

    private Integer lateMinutes;

    private Integer earlyMinutes;

    /** 工作日(1-7) */
    private String workDays;

    private Integer status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
