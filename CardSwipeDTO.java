package com.keshe.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class CardSwipeDTO {
    @NotBlank(message = "卡号不能为空")
    private String cardNo;

    @NotNull(message = "设备ID不能为空")
    private Long deviceId;

    /** 刷卡时间（可选，默认当前时间） */
    private String swipeTime;
}
