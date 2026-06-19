package com.keshe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("access_rule_device")
public class AccessRuleDevice {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ruleId;

    private Long deviceId;
}
