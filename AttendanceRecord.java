package com.keshe.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("attendance_record")
public class AttendanceRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long scheduleId;

    private String userName;

    private LocalDate date;

    private LocalDateTime checkInTime;

    private LocalDateTime checkOutTime;

    private String checkInDevice;

    private String checkOutDevice;

    /** 状态 0正常 1迟到 2早退 3迟到早退 4缺卡 5旷工 6休息 */
    private Integer status;

    private Integer lateMinutes;

    private Integer earlyMinutes;

    private BigDecimal workHours;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
