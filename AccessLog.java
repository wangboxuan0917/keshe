package com.keshe.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("access_log")
public class AccessLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String userName;

    private Long deviceId;

    private String deviceName;

    private String location;

    /** 通行类型 1进门 2出门 */
    private Integer accessType;

    /** 验证方式 1密码 2刷卡 3人脸 4远程 5二维码 6管理员开门 */
    private Integer verifyMode;

    /** 结果 1成功 2拒绝 3超时 4异常 */
    private Integer result;

    private String cardNo;

    private String remark;

    private LocalDateTime accessTime;
}
