package com.keshe.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("device_door")
public class DeviceDoor {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String code;

    private String location;

    private String building;

    private String floor;

    /** 设备类型 1门禁闸机 2门禁读卡机 3电梯读卡机 4教室签到机 5教师打卡机 */
    private Integer type;

    /** 用途 1普通门禁 2教师考勤点 3学生考勤点 */
    private Integer usageType;

    /** 性别限制 0不限 1男宿 2女宿 */
    private Integer genderLimit;

    /** 状态 0离线 1在线 2故障 */
    private Integer status;

    /** 门状态 0关闭 1开启 2异常 */
    private Integer doorStatus;

    private String ipAddress;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
