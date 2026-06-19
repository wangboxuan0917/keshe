package com.keshe.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("visitor_record")
public class VisitorRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long visitorId;

    private String visitorName;

    private String visitorPhone;

    private Long hostUserId;

    private String hostUserName;

    private Long deviceId;

    private String deviceName;

    private String reason;

    /** 状态 0待审批 1已批准 2已签到 3已完成 4已拒绝 5已过期 */
    private Integer status;

    private String qrCode;

    private String tempPassword;

    private LocalDateTime validStart;

    private LocalDateTime validEnd;

    private LocalDateTime actualStart;

    private LocalDateTime actualEnd;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
