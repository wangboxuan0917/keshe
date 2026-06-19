package com.keshe.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("class_schedule")
public class ClassSchedule {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String courseName;

    private String gradeClass;

    private String teacherName;

    private Long deviceId;

    private String deviceName;

    /** 星期(1-7) */
    private Integer dayOfWeek;

    private LocalTime startTime;

    private LocalTime endTime;

    private LocalDate validStart;

    private LocalDate validEnd;

    /** 状态 0禁用 1启用 */
    private Integer status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
