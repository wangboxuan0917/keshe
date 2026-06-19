package com.keshe.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("visitor")
public class Visitor {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String phone;

    private String idCard;

    private String cardNo;

    private String company;

    private String faceImage;

    /** 是否黑名单 0否 1是 */
    private Integer blacklist;

    private Integer visitCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
